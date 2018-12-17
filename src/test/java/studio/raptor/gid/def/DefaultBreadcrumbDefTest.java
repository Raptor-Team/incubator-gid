package studio.raptor.gid.def;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * DefaultBreadcrumbDef测试用例。
 *
 * @author bruce
 * @since 0.1
 */
public class DefaultBreadcrumbDefTest {

  @Test
  public void testToString() throws Exception {

    BreadcrumbDef def = new DefaultBreadcrumbDef() {
      @Override
      public String name() {
        return "default";
      }
    };

    System.out.println(def.toString());

  }

}