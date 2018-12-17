package studio.raptor.gid.common;


import org.junit.Assert;
import org.junit.Test;

/**
 * 键值对测试用例
 *
 * @author bruce
 * @since 0.1
 */
public class PairTest {

  @Test
  public void getKey() throws Exception {
    Pair<String,Integer> p1 = new Pair<>("key",1);
    Assert.assertEquals("key",p1.getKey());

    Pair<Integer,Integer> p2 = new Pair<>(1,1);
    Assert.assertEquals(1,p2.getKey().intValue());

    Pair<Integer,Integer> p3 = new Pair<>(null,1);
    Assert.assertEquals(null,p3.getKey());
  }

  @Test
  public void getValue() throws Exception {
    Pair<String,Integer> p1 = new Pair<>("key",1);
    Assert.assertEquals(1,p1.getValue().intValue());

    Pair<String,String> p2 = new Pair<>("key","value");
    Assert.assertEquals("value",p2.getValue());

    Pair<String,Integer> p3 = new Pair<>("key",null);
    Assert.assertEquals(null,p3.getValue());
  }

  @Test
  public void equals() throws Exception{
    Pair<String,Integer> p1 = new Pair<>("key",1);
    Pair<String,Integer> p2 = new Pair<>("key",1);
    Assert.assertEquals(p1,p1);
    Assert.assertEquals(p1,p2);

    Pair<String,String> p3 = new Pair<>("key","value");
    Pair<String,String> p4 = new Pair<>("key","value");
    Assert.assertEquals(p3,p3);
    Assert.assertEquals(p3,p4);

    Pair<String,Integer> p5 = new Pair<>("key",null);
    Pair<String,Integer> p6 = new Pair<>("key",null);
    Assert.assertEquals(p5,p5);
    Assert.assertEquals(p5,p6);

    Pair<String,Integer> p7 = new Pair<>("key",1);
    Pair<String,Integer> p8 = new Pair<>("key",null);
    Assert.assertNotEquals(p7,p8);
    Assert.assertEquals(p8,p8);
  }

}