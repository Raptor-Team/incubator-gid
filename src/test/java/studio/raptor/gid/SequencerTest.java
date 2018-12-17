/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package studio.raptor.gid;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.raptor.gid.common.CuratorUtil;
import studio.raptor.gid.common.GidException;
import studio.raptor.gid.common.VMHelper;
import studio.raptor.gid.def.BreadcrumbDef;
import studio.raptor.gid.kind.Sequencable;
import studio.raptor.gid.service.impl.CacheServiceImpl;
import studio.raptor.gid.service.interfaces.CacheService;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


/**
 * 序列发生器测试。
 *
 * @author bruce
 * @since 0.1
 */
public class SequencerTest {

  Logger logger = LoggerFactory.getLogger(SequencerTest.class);
  private static TestingServer server;
  private static CuratorFramework client;

  private String zkConnectStr;

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @BeforeClass
  public static void beforeClass() {
    try {
      server = new TestingServer();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void afterClass() {
    try {
      server.stop();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Before
  public void setUp() throws Exception {
    this.zkConnectStr = server.getConnectString();
    client = CuratorUtil.newClient(zkConnectStr, "testgid");
    client.blockUntilConnected();

    CuratorUtil.mkDirs(client, "/sequences");
    CuratorUtil.createPersistentNode(client, "/workers/127.0.0.1-8080", "5".getBytes());
//    CuratorUtil.createPersistentNode(client, "/workers/192.168.1.1-8888", "6".getBytes());
  }

  @After
  public void tearDown() {
    try {
      CuratorUtil.delDirs(client, "/workers");
      CuratorUtil.delDirs(client, "/sequences");
    } catch (Exception e) {
      e.printStackTrace();
    }
    client.close();
  }

  /**
   * 正确连上ZK+格式正确的全类型序列定义文件
   */
  @Test
  public void testCreateWithcorrectZk() throws Exception {
    Sequencer sequencer = new Sequencer(zkConnectStr, "test",
        "/xml/seq_test_well_formed_all.xml", "127.0.0.1", 8080);
    sequencer.startup();
  }

  /**
   * 错误连接ZK + 非breadcrumb的序列定义文件
   */
  @Test
  public void testCreateWithIncorrectZkAndNoBreadcrumb() throws Exception {
    VMHelper.setWorkerid("1");

    String incorrectZkConnectStr = "192.168.66.66:2181";
    Sequencer sequencer = new Sequencer(incorrectZkConnectStr, "test",
        "/xml/seq_test_well_formed_nobreadcrumb.xml", "127.0.0.1", 8080);
    sequencer.startup();
  }


  /**
   * 错误连接ZK + breadcrumb的序列定义文件
   */
  @Test
  public void testCreateWithIncorrectZkAndBreadcrumb() throws Exception {
    expectedEx.expect(Exception.class);

    VMHelper.setWorkerid("1");

    String incorrectZkConnectStr = "192.168.66.66:2181";
    Sequencer sequencer = new Sequencer(incorrectZkConnectStr, "test",
        "/xml/seq_test_well_formed_breadcrumb.xml", "127.0.0.1", 8080);
    sequencer.startup();
  }


  @Test
  public void testAdd() throws Exception {
    Sequencer sequencer = new Sequencer(zkConnectStr, "test", "/xml/seq_test_well_formed_all.xml",
        "127.0.0.1", 8080);
    sequencer.startup();
  }

  @Test
  public void testAdd1() throws Exception {
    Sequencer server = new Sequencer(zkConnectStr, "test", "/not_exist_file.xml", "192.168.1.1",
        8888);
    server.startup();
    server.add(new BreadcrumbDef() {

      @Override
      public long start() {
        return 0;
      }

      @Override
      public String name() {
        return "test_breadcrumb_id";
      }

      @Override
      public long incr() {
        return 10;
      }

      @Override
      public int cache() {
        return 100;
      }
    });

    assertThat(server.get("test_breadcrumb_id"), notNullValue());
  }

  @Test
  public void testAdd2() throws Exception {

    expectedEx.expect(GidException.class);
    expectedEx.expectMessage("exists");

    Sequencer server = new Sequencer(zkConnectStr, "test", "/not_exist_file.xml", "192.168.1.1",
        8888);
    server.startup();
    server.add(new BreadcrumbDef() {

      @Override
      public long start() {
        return 0;
      }

      @Override
      public String name() {
        return "test_breadcrumb_id";
      }

      @Override
      public long incr() {
        return 1;
      }

      @Override
      public int cache() {
        return 0;
      }
    });

    server.add(new BreadcrumbDef() {

      @Override
      public long start() {
        return 10;
      }

      @Override
      public String name() {
        return "test_breadcrumb_id";
      }

      @Override
      public long incr() {
        return 2;
      }

      @Override
      public int cache() {
        return 10;
      }
    });
  }

  @Test
  public void testGet1() throws Exception {
    expectedEx.expect(GidException.class);
    expectedEx.expectMessage("not exists");

    Sequencer server = new Sequencer(zkConnectStr, "test", "/not_exist_file.xml", "192.168.1.1",
        8888);

    server.startup();

    server.get("test_breadcrumb_id");
  }

  @Test
  public void testGet2() throws Exception {
    expectedEx.expect(GidException.class);
    expectedEx.expectMessage("NOT started");

    Sequencer server = new Sequencer(zkConnectStr, "test", "/not_exist_file.xml", "192.168.1.1",
            8888);
    server.get("test_breadcrumb_id");
    server.startup();
  }

  @Test
  public void testCache() throws Exception {
//    expectedEx.expect(GidException.class);
//    expectedEx.expectMessage("NOT started");

//    this.zkConnectStr="192.168.199.24:2181";
    Sequencer server = new Sequencer(zkConnectStr, "testgid", "/xml/seq_test_well_formed_all.xml", "127.0.0.1",
            8080);
    server.startup();

    System.out.println( "SequencerTest.testCache zkConnectStr="+zkConnectStr );

    Sequencable sequencable=server.get("breadcrumb2");
    CacheService cacheService=new CacheServiceImpl();
    for(int i=0;i<100;i++){
      if(i==16){
        cacheService.adjustCache( "breadcrumb2",22 );
      }
      if(i==50){
        cacheService.adjustCache( "breadcrumb2",18 );
      }
//      sequencable.nextId();
      logger.debug( "SequencerTest.testCache "+sequencable.nextId() );
    }





/*
    Sequencable sequencable2=server.get("breadcrumb2");
    System.out.println( "SequencerTest.testCache "+sequencable2.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable2.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable2.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable2.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable2.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable2.nextId() +"\n");

    Sequencable sequencable3=server.get("breadcrumb3");
    System.out.println( "SequencerTest.testCache "+sequencable3.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable3.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable3.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable3.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable3.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable3.nextId() +"\n");

    Sequencable sequencable4=server.get("breadcrumb4");
    System.out.println( "SequencerTest.testCache "+sequencable4.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable4.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable4.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable4.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable4.nextId() );
    System.out.println( "SequencerTest.testCache "+sequencable4.nextId() +"\n");*/

   /* Sequencable snowflake1=server.get("snowflake1");
    System.out.println( "SequencerTest.testCache "+snowflake1.nextId() );
    System.out.println( "SequencerTest.testCache "+snowflake1.nextId() );
    System.out.println( "SequencerTest.testCache "+snowflake1.nextId() );
    System.out.println( "SequencerTest.testCache "+snowflake1.nextId() );
    System.out.println( "SequencerTest.testCache "+snowflake1.nextId() );
    System.out.println( "SequencerTest.testCache "+snowflake1.nextId() +"\n");

    Sequencable snowflake2=server.get("snowflake2");
    System.out.println( "SequencerTest.testCache "+snowflake2.nextId() );
    System.out.println( "SequencerTest.testCache "+snowflake2.nextId() );
    System.out.println( "SequencerTest.testCache "+snowflake2.nextId() );
    System.out.println( "SequencerTest.testCache "+snowflake2.nextId() );
    System.out.println( "SequencerTest.testCache "+snowflake2.nextId() );
    System.out.println( "SequencerTest.testCache "+snowflake2.nextId() +"\n");

    Sequencable ticktock1=server.get("ticktock1");
    System.out.println( "SequencerTest.testCache "+ticktock1.nextId() );
    System.out.println( "SequencerTest.testCache "+ticktock1.nextId() );
    System.out.println( "SequencerTest.testCache "+ticktock1.nextId() );
    System.out.println( "SequencerTest.testCache "+ticktock1.nextId() );
    System.out.println( "SequencerTest.testCache "+ticktock1.nextId() );
    System.out.println( "SequencerTest.testCache "+ticktock1.nextId() +"\n");*/

  }
}
