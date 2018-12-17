package studio.raptor.gid.def;

import static org.junit.Assert.*;

import org.junit.Test;
import studio.raptor.gid.kind.Breadcrumb;

/**
 * BreadcrumDef测试用例。
 *
 * @author bruce
 * @since 0.1
 */
public class BreadcrumbDefTest {

  @Test
  public void testToString() throws Exception {
    BreadcrumbDef def = new BreadcrumbDef() {
      @Override
      public String name() {
        return "test";
      }

      @Override
      public int cache() {
        return 10;
      }

      @Override
      public long incr() {
        return 11110;
      }

      @Override
      public long start() {
        return 111110;
      }
    };

    System.out.println(def.toString());

  }

}