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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.raptor.gid.common.GidException;
import studio.raptor.gid.common.ThreadSafe;
import studio.raptor.gid.def.BreadcrumbDef;
import studio.raptor.gid.def.SequenceDef;

/**
 * 面包屑序列。
 *
 * 一、特性
 * 基于zookeeper生成的序列，序列值的生成类似于数据库的序列（Oralce的序列或MySQL的自增字段）。
 * 样例：1、2、3、4、5 ......
 *
 * 二、优缺点
 *【优】全局唯一、严格有序，可以完全替换数据库序列
 *【缺】依赖zookeeper，性能和可用性没有本地计算型序列高，序列值不能循环
 *
 * 三、使用场景
 * 传统使用数据库序列的场景
 *
 * @author bruce
 * @since 0.1
 */
@ThreadSafe
public class Breadcrumb extends ZookeeperSequence {

  public static final String SEQ_ROOT_PATH = "/raptor-sequences/";

  ////
  private static Logger log = LoggerFactory.getLogger(Breadcrumb.class);
  ////

  private BreadcrumbDef seqDef;

  private DistributedAtomicLong maxId;

  private IdBuffer buffer;

  private boolean isCached = false;

  /**
   * 构造函数
   *
   * @param seqDef 序列定义
   * @throws GidException 起始ID初始化异常
   */
  public Breadcrumb(final SequenceDef seqDef, CuratorFramework zkClient) throws GidException {
    super(zkClient);

    if (!this.isConnected) {
      throw new GidException("Zookeeper is NOT isConnected ");
    }

    this.seqDef = (BreadcrumbDef) seqDef;

    // 序列计数器
    this.maxId = new DistributedAtomicLong(zkClient,
        ZKPaths.makePath(SEQ_ROOT_PATH, seqDef.name()),
        new RetryNTimes(3, 200));

    // 设置起始值
    long startId = this.seqDef.start();
    try {
      if (!this.maxId.initialize(startId)) {
        startId = this.maxId.get().postValue().longValue();
        log.info("seq <" + this.seqDef.name() + "> 's curId is " + startId);
      }
    } catch (Exception e) {
      throw new GidException("initalize startid failure", e);
    }

    // 初始化缓冲区
    if (this.seqDef.cache() > 0) {
      isCached = true;
      buffer = new IdBuffer(this.maxId, this.seqDef);
    }

  }

  /**
   * 重置序列起始值
   * @return
   */
  @Override
  public void reset(long newStart) throws GidException {
    try{
      this.maxId.forceSet(newStart);
      if(null != this.buffer) {
        this.buffer.clear();
      }
    }catch (Exception e){
      throw new GidException("Reset sequence start error,", e);
    }
  }

  @Override
  public boolean adjustCache(int newCache) throws Exception {
    boolean result=false;
    if(isCached){
      buffer.adjustCache(newCache);
      result=true;
    }
    else{
      log.debug( "sequence "+this.seqDef.name()+" isCached = false" );
      isCached = true;
      buffer = new IdBuffer(this.maxId, this.seqDef, newCache);
      result=true;
    }
    return result;
  }

  @Override
  public long nextId() throws GidException {
    try {
      if (this.isCached) {
        return this.buffer.nextId();
      }

      boolean succeed;
      Long id = null;
      do {
        AtomicValue<Long> value = this.maxId.add(this.seqDef.incr());
        if (succeed = value.succeeded()) {
          id = value.preValue();
        }
      } while (!succeed);
      return id;
    } catch (Exception e) {
      throw new GidException("get <" + this.seqDef.name() + "> next id fail", e);
    }
  }

}
