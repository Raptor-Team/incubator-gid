package studio.raptor.gid.kind;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.junit.Assert;
import org.junit.Test;
import studio.raptor.gid.def.DefaultTicktockDef;
import studio.raptor.gid.def.SequenceDef;

/**
 * Ticktock测试用例。
 *
 * @author bruce
 * @since 0.1
 */
public class TicktockTest extends SnowflakeTest {

  /**
   * 测试生产ID的唯一性
   */
  @Test
  public void testNextIdUnique() throws Exception {

    SequenceDef sequenceDef = new DefaultTicktockDef() {
      @Override
      public String name() {
        return "DEFAULT_TT_TEST_01";
      }
    };

    final Ticktock ticktock = new Ticktock(sequenceDef, sysid, client);
    final Set<Long> ids = Collections.synchronizedSet(new HashSet<Long>());

    final int loopCount = 1000000;
    final CountDownLatch latch = new CountDownLatch(loopCount);
    callNextIdConcurrently(loopCount, latch, ticktock, ids);
    latch.await();//等待所有任务执行完毕

    Assert.assertTrue(ids.size() == loopCount);
  }

  /**
   * 测试生产ID的性能
   */
  @Test
  public void testNextIdPerformance() throws Exception {

    SequenceDef sequenceDef = new DefaultTicktockDef() {
      @Override
      public String name() {
        return "DEFAULT_TT_TEST_01";
      }
    };

    final Ticktock ticktock = new Ticktock(sequenceDef, sysid, client);

    final int loopCount = 1000000;
    final CountDownLatch latch = new CountDownLatch(loopCount);

    long start = System.currentTimeMillis();

    callNextIdConcurrently(loopCount, latch, ticktock);

    latch.await();//等待所有任务执行完毕

    long cost = System.currentTimeMillis() - start;
    long tps = loopCount / cost * 1000;
    System.out.println(
        "ticktock > " + coreSize + " threads call " + loopCount + " times,costs " + cost
            + " ms,tps " + tps);

  }


}