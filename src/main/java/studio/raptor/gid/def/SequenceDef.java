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
 * 序列实例定义。
 *
 * @author bruce
 * @since 0.1
 */
public interface SequenceDef {

  /**
   * 获取序列名称
   *
   * @return 序列名称
   */
  String name();

  /**
   * 获取序列类型
   *
   * @return 序列类型
   * @see Type
   */
  Type type();

  /**
   * 校验序列定义的有效性
   *
   * @throws GidException 校验异常
   */
  void validate() throws GidException;

}
