package studio.raptor.gid.kind;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.raptor.gid.common.CuratorUtil;
import studio.raptor.gid.common.ExecutorUtil;
import studio.raptor.gid.def.DefaultSnowflakeDef;
import studio.raptor.gid.def.SequenceDef;

/**
 * Snowflake单元测试
 *
 * @author bruce
 * @since 0.1
 */
public class SnowflakeTest {

  public static String sysid = "localhost_test_4_snowflake";
  public static int coreSize = Runtime.getRuntime().availableProcessors();
  public static CuratorFramework client;
  public static ExecutorService executor = ExecutorUtil
      .createFixedThreadPool("testExecutor", coreSize);
  public static SequenceDef sequenceDef;
  private static TestingServer server;

  @BeforeClass
  public static void beforeClass() throws Exception {
    // 序列定义
    sequenceDef = new DefaultSnowflakeDef() {
      @Override
      public String name() {
        return "DEFAULT_SN_TEST_01";
      }
    };

    // 创建ZK服务端和客户端
    server = new TestingServer();
    client = CuratorUtil.newClient(server.getConnectString(), "test");
    client.blockUntilConnected();

    // 创建工作节点路径
    CuratorUtil.createPersistentNode(client, Snowflake.WK_ROOT_PATH + sysid, "1".getBytes());
  }

  @AfterClass
  public static void afterClass() {
    try {
      server.stop();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void callNextIdConcurrently(int loopCount, final CountDownLatch latch,
      final Snowflake snowflake) {
    callNextIdConcurrently(loopCount, latch, snowflake, null);
  }

  public static void callNextIdConcurrently(int loopCount, final CountDownLatch latch,
      final Snowflake snowflake, final Set<Long> ids) {

    for (int i = 0; i < loopCount; ++i) {

      executor.submit(new Runnable() {
        @Override
        public void run() {
          try {
            long start = System.currentTimeMillis();
            long id = snowflake.nextId();
            long end = System.currentTimeMillis();

            if (end - start > 2) {
              System.out.println(id + " - " + (end - start));
            }

            if (null != ids) {
              ids.add(id);
            }

          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            latch.countDown();
          }
        }
      });
    }
  }

  /**
   * 测试生产ID的唯一性
   */
  @Test
  public void testNextIdUnique() throws Exception {
    final Snowflake snowflake = new Snowflake(sequenceDef, sysid, client);
    final Set<Long> ids = Collections.synchronizedSet(new HashSet<Long>());

    final int loopCount = 10000000;
    final CountDownLatch latch = new CountDownLatch(loopCount);

    callNextIdConcurrently(loopCount, latch, snowflake, ids);

    latch.await();//等待所有任务执行完毕

    Assert.assertTrue(ids.size() == loopCount);
  }

  /**
   * 测试生产ID的性能
   */
  @Test
  public void testNextIdPerformance() throws Exception {
    final Snowflake snowflake = new Snowflake(sequenceDef, sysid, client);

    final int loopCount = 10000000;
    final CountDownLatch latch = new CountDownLatch(loopCount);

    long start = System.currentTimeMillis();

    callNextIdConcurrently(loopCount, latch, snowflake);

    latch.await();//等待所有任务执行完毕

    long cost = System.currentTimeMillis() - start;
    long tps = loopCount / cost * 1000;
    System.out.println(
        "snowflake -> " + coreSize + " threads call " + loopCount + " times,costs " + cost
            + " ms,tps " + tps);

  }

}