package studio.raptor.gid.kind;

import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.raptor.gid.common.CuratorUtil;
import studio.raptor.gid.common.GidException;

/**
 * 依赖于zookeeper的远程序列抽象类
 * <p>
 * 依赖远程ZK保持工作节点编码或发放序列许可
 * </p>
 *
 * @author bruce
 * @since 0.1
 */
public abstract class ZookeeperSequence implements Sequencable {

  private static final Logger log = LoggerFactory.getLogger(ZookeeperSequence.class);

//  /**
//   * 工作节点路径@zk
//   */
//  public static final String WK_ROOT_PATH = "/workers/";

  /**
   * zk客户端
   */
  public CuratorFramework zkClient = null;

  /**
   * 是否已连接至远端
   */
  public boolean isConnected = false;

//  /**
//   * 工作节点编码
//   */
//  public long workerId = -1;


  public ZookeeperSequence(CuratorFramework zkClient) throws GidException {

    this.zkClient = zkClient;

    try {
      this.isConnected = this.zkClient
          .blockUntilConnected(CuratorUtil.DEFAULT_CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      log.info("Connected to remote zookeeper {} {}", this.zkClient.getZookeeperClient().getCurrentConnectionString(), this.isConnected);
    } catch (InterruptedException e) {
      e.printStackTrace();
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void reset(long newStart) throws GidException {
    throw new GidException("This type of sequence not support the method reset().");
  }

  @Override
  public boolean adjustCache(int newCache) throws Exception {
    throw new GidException("This type of sequence not support the method adjustCache().");
  }
}
