package studio.raptor.gid.kind;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.raptor.gid.common.ExecutorUtil;
import studio.raptor.gid.common.OnewayLinkedBlockingQueue;
import studio.raptor.gid.common.GidException;
import studio.raptor.gid.def.BreadcrumbDef;

/**
 * ID缓冲器。
 *
 * <ol>
 *     <li>根据缓冲区空位比率控制ID的提前填充，确保缓冲区能持续不间断为上层提供ID</li>
 *     <li>当空位比率超出阈值触发填充操作时，只允许一个填充任务执行避免并发</li>
 *     <li>从缓冲区获取ID采用超时策略，保障ID使用方不会因长时间等待而阻塞</li>
 * </ol>
 *
 * @author bruce
 * @since 0.1
 */
public class IdBuffer {

  private static Logger log = LoggerFactory.getLogger(IdBuffer.class);

  /**
   * 用于执行ID申请的线程池
   */
  public static ExecutorService idWorkerExecutor = ExecutorUtil.createCachedThreadPool("id_worker", 1, Runtime.getRuntime().availableProcessors());

  /**
   * ID缓冲区空闲比率
   */
  private static double id_pool_vacancy_rate = 0.3;

  /**
   * 从缓冲区获取ID的超时时间（毫秒）
   */
  private static int get_id_timeout_ms = 100;

  /**
   * ID池
   */
  private OnewayLinkedBlockingQueue<Long> idPool;

  /**
   * ID最大值许可（@zk）
   */
  private DistributedAtomicLong maxId;

  /**
   * 序列定义
   */
  private BreadcrumbDef seqDef;


  /**
   * 从远端申请ID时的空闲阈值（ID缓冲区允许的最大空闲阀值）
   */
  private long applicantThreshold;

  /**
   * 是否正在申请远端ID
   * 确保同时只能有一个申请任务在运行
   */
  private AtomicBoolean isApplying;

  /**
   * 应用启动时的初始缓冲大小
   */
  private int initPoolSize;

  /**
   * 动态调整的cache值
   */
  private AtomicInteger dynamicPoolSize=new AtomicInteger( 0 );

  /**
   * 构造函数
   *
   * @param maxId 最大ID计数器
   * @param seqDef 序列定义
   */
  public IdBuffer(DistributedAtomicLong maxId, BreadcrumbDef seqDef) {
    this.maxId = maxId;
    this.seqDef = seqDef;
    this.initPoolSize=this.seqDef.cache();
    log.info("this.initPoolSize="+ this.initPoolSize+" this.dynamicPoolSize="+this.dynamicPoolSize );
    if(this.dynamicPoolSize.get()==0){
      this.dynamicPoolSize.set(this.initPoolSize);
    }
    this.idPool = new OnewayLinkedBlockingQueue<Long>(this.initPoolSize);
    acquire(this.initPoolSize);//申请填充ID缓冲区
    //缓冲大小*空闲比例
    this.applicantThreshold = Math.max((long) Math.floor(this.initPoolSize * id_pool_vacancy_rate), 1);
    this.isApplying = new AtomicBoolean(false);
  }

  /**
   * 构造函数
   *
   * @param maxId 最大ID计数器
   * @param seqDef 序列定义
   * @param newCache cache值
   */
  public IdBuffer(DistributedAtomicLong maxId, BreadcrumbDef seqDef,int newCache) {
    this.maxId = maxId;
    this.seqDef = seqDef;
    this.initPoolSize=newCache;
    log.info("this.initPoolSize="+ this.initPoolSize+" this.dynamicPoolSize="+this.dynamicPoolSize );
    if(this.dynamicPoolSize.get()==0){
      this.dynamicPoolSize.set(this.initPoolSize);
    }
    this.idPool = new OnewayLinkedBlockingQueue<Long>(this.initPoolSize);
    acquire(this.initPoolSize);//申请填充ID缓冲区
    //缓冲大小*空闲比例
    this.applicantThreshold = Math.max((long) Math.floor(this.initPoolSize * id_pool_vacancy_rate), 1);
    this.isApplying = new AtomicBoolean(false);
  }

  /**
   * 获取ID
   */
  public Long nextId() throws GidException {
    if(!this.isCacheChanged()){
      // id池中空闲位数量超过指定阈值 且 idBuffer未处在申请状态
      if ((this.idPool.remainingCapacity() >= this.applicantThreshold)
              && (this.isApplying.compareAndSet(false, true))) {
        idWorkerExecutor.submit(new IdWorker(this.idPool.remainingCapacity()));
      }
    }else{
      //cache调整过,等待上一个idPool全部用完，才开启下一个新size的idPool
      log.info( "this.idPool.remainingCapacity()="+this.idPool.remainingCapacity() );
      if(this.idPool.remainingCapacity()==this.initPoolSize){
//        this.idPool.clear();
        this.initPoolSize=this.dynamicPoolSize.get();
        this.idPool = new OnewayLinkedBlockingQueue<Long>(this.initPoolSize);
        //缓冲大小*空闲比例
        this.applicantThreshold = Math.max( (long) Math.floor( this.initPoolSize * id_pool_vacancy_rate ), 1 );
        if(this.isApplying.compareAndSet(false, true)) {
          // 申请填充ID缓冲区,通过线程池获取
          //acquire方法会和和fillIdPool.put方法会造成死锁
//        acquire(this.initPoolSize);
        idWorkerExecutor.submit(new IdWorker(this.initPoolSize));
          //初始化1/3
//          int num=Math.max(this.initPoolSize/3,1);
//          idWorkerExecutor.submit(new IdWorker(num));
        }
      }
    }

    Long id;
    try {
      id = idPool.poll(get_id_timeout_ms, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new GidException(e);
    }

    if (null == id) {
      throw new GidException("Id pool is empty ,maybe too fast to get");
    }

    return id;
  }

  public void clear(){
    this.idPool.clear();
  }

  /**
   * 向远端获取ID
   *
   * @param num 申请的ID数量
   */
  private void acquire(int num) {
    log.info("acquire sequence "+this.seqDef.name()+ " acquire:"+num+" initPoolSize="+this.initPoolSize+" dynamicPoolSize="+this.dynamicPoolSize.get() );
    AtomicValue<Long> rc = null;
    // 增加maxId直到成功 TODO v0.2 将根据ZK ERROR进行有限尝试
    do {
      try {
        //增长步长值*申请数量
        long delta = seqDef.incr() * num;
        rc = maxId.add(delta);
      } catch (Exception e) {
        log.warn("Apply for adding " + num + " to " + seqDef.name() + " failure", e);
      }
    } while (!rc.succeeded());

    fillIdPool(rc.preValue(), rc.postValue(), seqDef.incr());
    log.info("acquire sequence finished");
  }

  /**
   * 填充ID
   *
   * @param start 开始ID
   * @param end 结束ID
   * @param step 步长
   */
  private void fillIdPool(long start, long end, long step) {
    log.info("Fill id pool ：range -> [{},{}),total -> {}", start, end, end - start);
    long id = start;
    do {
      //对于put方法，若向队尾添加元素的时候发现队列已经满了会发生阻塞一直等待空间，以加入元素
      idPool.put(id);
      id += step;
    } while (id < end);
    log.info( "Fill id pool finished" );
  }

  /**
   * 判断 cache值是否修改过
   * @return
   */
  private boolean isCacheChanged(){
    if(this.dynamicPoolSize.get() != this.initPoolSize) return true;
    return false;
  }

  /**
   * 调整cache值
   * @param newCache
   */
  public void adjustCache(int newCache){
    this.dynamicPoolSize.set(newCache );
  }

  /**
   * ID生成器
   */
  private class IdWorker implements Runnable {

    private int shortage; // 短缺的ID数据量

    public IdWorker(int shortage) {
      this.shortage = shortage;
    }

    @Override
    public void run() {
      try {
        acquire(this.shortage);
      } catch (Exception e) {
        log.error("Id worker run acquiring task failure", e);
      } finally {
        isApplying.set(false);
      }
    }
  }

}
