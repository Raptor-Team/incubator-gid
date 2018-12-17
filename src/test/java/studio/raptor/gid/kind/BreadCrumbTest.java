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

package studio.raptor.gid.kind;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.raptor.gid.common.CuratorUtil;
import studio.raptor.gid.common.ExecutorUtil;
import studio.raptor.gid.common.GidException;
import studio.raptor.gid.def.BreadcrumbDef;
import studio.raptor.gid.def.SequenceDef;

/**
 * 面包屑序列测试用例
 *
 * @author bruce
 * @since 0.1
 */
public class BreadCrumbTest {

  private static TestingServer server;

  private static CuratorFramework client;

  private static SequenceDef bc_aid_without_cache;

  private static SequenceDef bc_aid_with_cache;


  @BeforeClass
  public static void beforeClass() throws InterruptedException {
    try {
      server = new TestingServer();
    } catch (Exception e) {
      e.printStackTrace();
    }

    client = CuratorUtil.newClient(server.getConnectString(), "test");
    client.blockUntilConnected();
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
  public void setup() throws Exception {

    bc_aid_without_cache = new BreadcrumbDef() {
      @Override
      public String name() {
        return "aid";
      }

      @Override
      public int cache() {
        return 0;
      }

      @Override
      public long incr() {
        return 1;
      }

      @Override
      public long start() {
        return 0;
      }
    };

    bc_aid_with_cache = new BreadcrumbDef() {
      @Override
      public String name() {
        return "aid";
      }

      @Override
      public int cache() {
        return 10000;
      }

      @Override
      public long incr() {
        return 1;
      }

      @Override
      public long start() {
        return 0;
      }
    };

  }

  @After
  public void teardown() throws Exception {
    if (null != server) {
      server.stop();
    }
  }

  @Test
  public void testNextidWithoutCache() throws Exception {

    int threadNum = 6;

    final Breadcrumb breadcrumb = new Breadcrumb(bc_aid_without_cache, client);
    final CountDownLatch latch = new CountDownLatch(threadNum);

    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger failureCount = new AtomicInteger(0);

    long start = System.currentTimeMillis();
    for (int i = 0; i < threadNum; i++) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          for (int i = 0; i < 10000; ++i) {
            long s = System.currentTimeMillis();
            try {
              long id = breadcrumb.nextId();
              long threadId = Thread.currentThread().getId();
              System.out.println(threadId + "-" + id + "-" + (System.currentTimeMillis()-s));
              successCount.incrementAndGet();
            } catch (GidException e) {
              e.printStackTrace();
              failureCount.incrementAndGet();
            }
          }
          latch.countDown();
        }
      }).start();
    }

    latch.await();
    System.out.println("cost:" + (System.currentTimeMillis() - start));
    System.out.println("successCount:" + successCount.get() + ",failureCount:" + failureCount);
  }

  @Test
  public void testNextidWithoutCache2() throws Exception {

    final int threadNum = 6;

    final CountDownLatch latch = new CountDownLatch(threadNum);

    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger failureCount = new AtomicInteger(0);

    final Breadcrumb[] breadcrumbs = new Breadcrumb[threadNum];
    for (int i = 0; i < threadNum; i++) {
      breadcrumbs[i] = new Breadcrumb(bc_aid_without_cache, client);
    }

    long start = System.currentTimeMillis();
    for (int i = 0; i < threadNum; i++) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          for (int j = 0; j < 10000; ++j) {
            try {
              long s = System.currentTimeMillis();
              long id = breadcrumbs[j % threadNum].nextId();
              long threadId = Thread.currentThread().getId();
              System.out.println(threadId + "-" + id + "-" + (System.currentTimeMillis()-s));
              successCount.incrementAndGet();
            } catch (Exception e) {
              System.out.println(e.getMessage());
              failureCount.incrementAndGet();
            }
          }
          latch.countDown();
        }
      }).start();
    }

    latch.await();
    System.out.println("cost:" + (System.currentTimeMillis() - start));
    System.out.println("successCount:" + successCount.get() + ",failureCount:" + failureCount);
  }


  @Test
  public void testNextidWithCache() throws Exception {

    final int threadNum = 6;

    final CountDownLatch latch = new CountDownLatch(threadNum);

    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger failureCount = new AtomicInteger(0);

    final Breadcrumb breadcrumb = new Breadcrumb(bc_aid_with_cache, client);

    long start = System.currentTimeMillis();
    for (int i = 0; i < threadNum; i++) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          for (int i = 0; i < 100000; ++i) {
            try {
              long s = System.currentTimeMillis();
              long id = breadcrumb.nextId();
              long threadId = Thread.currentThread().getId();
              System.out.println(threadId+ "-" + id + "-" + (System.currentTimeMillis()-s));
              successCount.incrementAndGet();
            } catch (GidException e) {
              System.out.println(e.getMessage());
              failureCount.incrementAndGet();
            }
          }
          latch.countDown();
        }
      }).start();
    }

    latch.await();
    System.out.println("cost:" + (System.currentTimeMillis() - start));
    System.out.println("successCount:" + successCount.get() + ",failureCount:" + failureCount);
  }

  @Test
  public void testNextidWithCache2()
      throws GidException, InterruptedException, ExecutionException {

    final int proccessors = 100; //模拟的序列获取进程数
    final int concurrents = 5000; //模拟单个序列实例的并发操作数

    final CompletionService<Long> executor = new ExecutorCompletionService<>(
        ExecutorUtil.createFixedThreadPool("workers", proccessors));
    final Set<Long> idSet = new HashSet<>();
    final CountDownLatch latch = new CountDownLatch(proccessors * concurrents);

    final AtomicInteger count = new AtomicInteger(0);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger failureCount = new AtomicInteger(0);

    final Breadcrumb[] breadcrumbs = new Breadcrumb[proccessors];
    for (int i = 0; i < proccessors; i++) {
      breadcrumbs[i] = new Breadcrumb(bc_aid_with_cache, client);
    }

    long start = System.currentTimeMillis();
    for (int i = 0; i < proccessors; ++i) {
      final Breadcrumb breadcrumb = breadcrumbs[i];
      for (int j = 0; j < concurrents; ++j) {
        executor.submit(new SeqConsumingTask(breadcrumb, latch, count, successCount, failureCount));
      }
    }

    latch.await();
    long cost = System.currentTimeMillis() - start;

    System.out.println("cost:" + cost);
    System.out.println(
        "count:" + count.get() + ",successCount:" + successCount.get() + ",failureCount:"
            + failureCount.get());
    System.out.println("tps:" + (count.get() * 1000 / cost));

    for (int i = 0; i < proccessors * concurrents; ++i) {
      try {
        idSet.add(executor.take().get());
      } catch (Exception e) {
        //
      }
    }

    Assert.assertEquals(successCount.get(), idSet.size());
  }


  class SeqConsumingTask implements Callable<Long> {

    final Breadcrumb bc;
    CountDownLatch latch;

    AtomicInteger count;
    AtomicInteger successCount;
    AtomicInteger failureCount;

    SeqConsumingTask(Breadcrumb bc, CountDownLatch latch, AtomicInteger count,
        AtomicInteger successCount,
        AtomicInteger failureCount) {
      this.bc = bc;
      this.latch = latch;
      this.count = count;
      this.successCount = successCount;
      this.failureCount = failureCount;
    }

    @Override
    public Long call() throws GidException {
      try {
        return getId();
      } catch (Exception e) {
        throw e;
      } finally {
        latch.countDown();
      }
    }

    private Long getId() throws GidException {
      this.count.incrementAndGet();
      long start = System.currentTimeMillis();
      try {
        long id = bc.nextId();
        this.successCount.incrementAndGet();
        return id;
      } catch (GidException e) {
        this.failureCount.incrementAndGet();
        throw e;
      } finally {
        long end = System.currentTimeMillis() - start;
        if (end > 100) {
          System.out.println(System.currentTimeMillis() - start);
        }
      }
    }
  }

}
