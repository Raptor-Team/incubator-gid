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

package studio.raptor.gid.common;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工具类。
 *
 * @author bruce
 * @since 0.1
 */
public class ExecutorUtil {

  /**
   * 线程池容器（用于防止创建重复的线程池）
   */
  public static Map<String, ExecutorService> executors = new ConcurrentHashMap<String, ExecutorService>();

  /**
   * 创建只有一个线程的线程池
   *
   * @param name 线程池名称
   * @return executorService
   */
  public static final ExecutorService createSingleThreadPool(String name) {
    Preconditions.checkArgument(!executors.containsKey(name), "%s executor exists", name);
    ExecutorService executorService = Executors
        .newSingleThreadExecutor(new NameableThreadFactory(name, true));
    executors.put(name, executorService);
    return executorService;
  }

  /**
   * 创建固定大小的线程池
   *
   * @param name 线程池名称
   * @param size 线程池大小
   * @return executorService
   */
  public static final ExecutorService createFixedThreadPool(String name, int size) {
    return createFixedThreadPool(name, size, true);
  }

  /**
   * 创建固定大小的线程池
   *
   * @param name 线程池名称
   * @param size 线程池大小
   * @param isDaemon 是否是守护线程
   * @return executorService
   */
  public static final ExecutorService createFixedThreadPool(String name, int size,
      boolean isDaemon) {
    Preconditions.checkArgument(!executors.containsKey(name), "%s executor exists", name);
    ExecutorService executorService = Executors
        .newFixedThreadPool(size, new NameableThreadFactory(name, isDaemon));
    executors.put(name, executorService);
    return executorService;
  }

  /**
   * 创建动态大小的线程池
   *
   * @param name 线程池名称
   * @return executorService
   */
  public static final ExecutorService createCachedThreadPool(String name) {
    return createCachedThreadPool(name, true);
  }

  /**
   * 创建动态大小的线程池
   *
   * @param name 线程池名称
   * @param isDaemon 是否是守护线程
   * @return executorService
   */
  public static final ExecutorService createCachedThreadPool(String name, boolean isDaemon) {
    Preconditions.checkArgument(!executors.containsKey(name), "%s executor exists", name);
    ExecutorService executorService = Executors
        .newCachedThreadPool(new NameableThreadFactory(name, isDaemon));
    executors.put(name, executorService);
    return executorService;
  }

  /**
   * 创建动态大小的线程池
   *
   * @param name 线程池名称
   * @param coreSize 核心线程数量
   * @param maxSize  最大线程数量
   * @return executorService
   */
  public static final ExecutorService createCachedThreadPool(String name, int coreSize,
      int maxSize) {
    Preconditions.checkArgument(!executors.containsKey(name), "%s executor exists", name);
    ExecutorService executorService = new ThreadPoolExecutor(coreSize,
        maxSize,
        30L,
        TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(),// 线程数可伸缩
        new NameableThreadFactory(name, true),
        new ThreadPoolExecutor.CallerRunsPolicy());
    executors.put(name, executorService);
    return executorService;
  }

  /**
   * 可命名的线程工厂
   */
  private static class NameableThreadFactory implements ThreadFactory {

    private final ThreadGroup group;
    private final String namePrefix;
    private final AtomicInteger threadId;
    private final boolean isDaemon;

    public NameableThreadFactory(String name, boolean isDaemon) {
      SecurityManager s = System.getSecurityManager();
      this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      this.namePrefix = name;
      this.threadId = new AtomicInteger(0);
      this.isDaemon = isDaemon;
    }

    public Thread newThread(Runnable r) {
      Thread t = new Thread(group, r, namePrefix + threadId.getAndIncrement());
      t.setDaemon(isDaemon);
      if (t.getPriority() != Thread.NORM_PRIORITY) {
        t.setPriority(Thread.NORM_PRIORITY);
      }
      return t;
    }
  }

}
