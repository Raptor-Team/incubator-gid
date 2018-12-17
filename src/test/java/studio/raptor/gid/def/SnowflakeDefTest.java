package studio.raptor.gid.def;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * SnowflakeDef测试用例。
 *
 * @author bruce
 * @since 0.1
 */
public class SnowflakeDefTest {

  @Test
  public void testToString() throws Exception {
      SnowflakeDef def = new SnowflakeDef() {
        @Override
        public String name() {
          return "test";
        }

        @Override
        public int workerIdWidth() {
          return 10;
        }

        @Override
        public int sequenceWidth() {
          return 12;
        }
      };

      System.out.println(def.toString());
  }

}