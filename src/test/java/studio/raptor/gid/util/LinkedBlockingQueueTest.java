package studio.raptor.gid.util;

import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Before;
import org.junit.Test;

/**
 * 功能描述
 *
 * @author Charley
 * @since 1.0
 */
public class LinkedBlockingQueueTest {

  private LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();

  @Before
  public void setUp() {
    try {
      queue.put(10);
      queue.put(20);
      queue.put(30);
      queue.put(40);
      queue.put(50);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testClear(){
    queue.clear();
  }

}
