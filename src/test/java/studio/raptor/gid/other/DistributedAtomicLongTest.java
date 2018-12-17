package studio.raptor.gid.other;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * ZK工具包测试类
 *
 * @author bruce
 * @since 0.0
 */
public class DistributedAtomicLongTest {


  private static final String PATH = "/examples/maxId";

  private TestingServer server;

  private CuratorFramework client;

  private DistributedAtomicLong counter;


  @Before
  public void setup() throws Exception {
    server = new TestingServer();
    server.start();
    client = CuratorFrameworkFactory
        .newClient(server.getConnectString(), new ExponentialBackoffRetry(1000, 3));
    client.start();

    counter = new DistributedAtomicLong(client, PATH, new RetryOneTime(100));
  }

  @After
  public void teardown() throws IOException {
    server.close();
    client.close();
  }

  @Test
  public void testInit1() throws Exception {
    Assert.assertTrue(counter.initialize(1L));
    Assert.assertEquals(1L, counter.get().postValue().longValue());

    //不能多次初始化
    Assert.assertFalse(counter.initialize(2L));
    Assert.assertEquals(1L, counter.get().postValue().longValue());

  }

  @Test
  public void testInit2() throws Exception {
    System.out.println(counter.initialize(1L));
    System.out.println(counter.get().preValue());
    System.out.println(counter.get().postValue());
  }

  @Test
  public void testNew() throws Exception {
    System.out.println(counter.initialize(1L));
    DistributedAtomicLong counter2 = new DistributedAtomicLong(client, PATH, new RetryOneTime(100));
    Assert.assertEquals(1L, counter2.get().postValue().longValue());
  }

  @Test
  public void testSet() throws Exception {
    System.out.println(counter.get().preValue());
    System.out.println(counter.get().postValue());
    System.out.println(counter.get().succeeded());
  }

  @Test
  public void testCAS() throws Exception {

    Assert.assertTrue(counter.initialize(1L));
    AtomicValue<Long> value = counter.compareAndSet(1L, 100L);
    Assert.assertTrue(value.succeeded());
    Assert.assertEquals(100L, value.postValue().longValue());

  }


  @Test
  public void testAdd() throws Exception {
    ExecutorService service = Executors.newFixedThreadPool(20);

    for (int i = 0; i < 100; i++) {
      Callable<Void> task = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          try {
            AtomicValue<Long> value = counter.add(2l);
            System.out.println("succeed: " + value.succeeded());
            if (value.succeeded()) {
              System.out.println("add: from " + value.preValue() + " to " + value.postValue());
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
      };
      service.submit(task);
    }

    service.shutdown();
    service.awaitTermination(10, TimeUnit.MINUTES);
  }


}
