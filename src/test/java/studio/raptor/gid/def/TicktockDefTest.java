package studio.raptor.gid.def;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * TicktockDef测试用例。
 *
 * @author bruce
 * @since 0.1
 */
public class TicktockDefTest {

  @Test
  public void testToString() throws Exception {
    TicktockDef def = new TicktockDef() {
      @Override
      public String name() {
        return "test";
      }

      @Override
      public int workerIdWidth() {
        return 2;
      }

      @Override
      public int sequenceWidth() {
        return 5;
      }
    };

    System.out.println(def.toString());
  }

}