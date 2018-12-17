package studio.raptor.gid.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import studio.raptor.gid.common.ExecutorUtil;
import studio.raptor.gid.common.OnewayLinkedBlockingQueue;

/**
 * 支持并发读阻塞的队列的测试
 *
 * @author bruce
 * @since 0.1
 */
public class ReadBlockingQueueTest {

  private static int coreNum = Runtime.getRuntime().availableProcessors();

  private static ExecutorService executor = ExecutorUtil.createCachedThreadPool("ReadBlockingQueueTest",coreNum,10*coreNum+1);

  private OnewayLinkedBlockingQueue<Long> queue = null;

  @Before
  public void setUp() throws Exception {
    queue = new OnewayLinkedBlockingQueue<>(10000);
  }

  @After
  public void tearDown() throws Exception {
    queue = null;
  }

  @Test
  public void size() throws Exception {
    assertEquals(0, queue.size());
    for (int i = 0; i < 10000; i++) {
      assertEquals(i, queue.size());
      queue.put(1L);
    }

  }

  @Test
  public void remainingCapacity() throws Exception {
    for (int i = 0; i < 10000; i++) {
      assertEquals(i, queue.size());
      queue.put(1L);
    }
    assertEquals(0, queue.remainingCapacity());

    for (int i = 0; i < 10000; i++) {
      queue.poll(1, TimeUnit.MILLISECONDS);
      assertEquals(i + 1, queue.remainingCapacity());
    }
  }

  @Test
  public void put() throws Exception {
    for (int i = 0; i < 10000; i++) {
      final long v = (long) i;
      queue.put(v);
    }

    assertEquals(10000, queue.size());

    for (int i = 0; i < 10000; i++) {
      Long x = queue.poll(1000, TimeUnit.MILLISECONDS);
      assertEquals((long) i, x.longValue());
    }

  }

  @Test
  public void poll() throws Exception {
    Long x = queue.poll(1000, TimeUnit.MILLISECONDS);
    assertTrue(null == x);

    for (int i = 0; i < 10000; i++) {
      final long v = (long) i;
      queue.put(v);
    }
    assertEquals(10000,queue.size());

    final AtomicInteger counter = new AtomicInteger();
    final CountDownLatch latch = new CountDownLatch(10000);
    final Random random = new Random(1000);

    for (int i = 0; i < 10000; i++) {
      executor.submit(new Runnable() {
        @Override
        public void run() {
          try {
            //LockSupport.parkNanos(random.nextLong() * 1000);
            //LockSupport.parkUntil(new Date().getTime() + random.nextLong() * 1000);

            Long x = queue.poll(10, TimeUnit.MILLISECONDS);
            if (null != x) {
              counter.incrementAndGet();
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }finally {
            latch.countDown();
            System.out.println(Thread.currentThread().getId()+"'s task is over");
          }
        }
      });
    }

    latch.await();

    assertEquals(10000l, counter.get());
  }

}