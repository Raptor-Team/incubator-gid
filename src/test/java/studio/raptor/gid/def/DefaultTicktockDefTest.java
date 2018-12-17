package studio.raptor.gid.def;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * DefaultTicktockDef测试用例。
 *
 * @author bruce
 * @since 0.1
 */
public class DefaultTicktockDefTest {

  @Test
  public void testToString() throws Exception {
    TicktockDef def = new DefaultTicktockDef() {
      @Override
      public String name() {
        return "default";
      }
    };

    System.out.println(def.toString());
  }

}