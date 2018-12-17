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

package studio.raptor.gid.def;

import studio.raptor.gid.common.GidException;
import studio.raptor.gid.common.Type;

/**
 * 面包屑型序列实例定义。
 *
 * @author bruce
 * @since 0.1
 */
public abstract class BreadcrumbDef implements SequenceDef {

  public BreadcrumbDef() throws GidException {
    validate();
  }

  /**
   * 获取序列名称
   *
   * @return 序列名称
   */
  public abstract String name();

  @Override
  public Type type() {
    return Type.BREADCRUMB;
  }

  /**
   * 获取缓冲大小
   *
   * @return 缓冲大小
   */
  public abstract int cache();

  /**
   * 获取增长步长
   *
   * @return 增长步长值
   */
  public abstract long incr();

  /**
   * 获取起始值
   *
   * @return 起始值
   */
  public abstract long start();


  @Override
  public void validate() throws GidException {
    if (cache() < 0) {
      throw new GidException(
          name() + " -> cache @breadcrumDdef must be equal or greater than 0");
    }

    if (incr() == 0) {
      throw new GidException(name() + " -> incr @breadcrumbDef must be unequal 0");
    }
  }

  @Override
  public String toString() {
    return String.format("sequenceDef[name=%s" + ",type=%s" + ",cache=%s" + ",incr=%s" + ",start=%s]",
        name(),
        type().name,
        cache(), incr(), start());

  }
}
