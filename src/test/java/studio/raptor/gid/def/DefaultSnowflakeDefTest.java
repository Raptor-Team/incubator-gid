package studio.raptor.gid.def;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * DefaultSnowflakeDef测试用例。
 *
 * @author bruce
 * @since 0.1
 */
public class DefaultSnowflakeDefTest {

  @Test
  public void testToString() throws Exception {

    SnowflakeDef def = new DefaultSnowflakeDef() {
      @Override
      public String name() {
        return "default";
      }
    };

    System.out.println(def.toString());
  }

}