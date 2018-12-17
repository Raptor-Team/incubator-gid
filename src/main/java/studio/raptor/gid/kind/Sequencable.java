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

import studio.raptor.gid.common.GidException;

/**
 * 可排列的
 * <p>
 * 数学上，序列是被排成一列的对象（或事件）；这样每个元素不是在其他元素之前，就是在其他元素之后。这里，元素之间的顺序非常重要。
 *
 * @author bruce shi
 */
public interface Sequencable {


  /**
   * 生产下一个序列值
   *
   * @return
   * @throws Exception
   */
  long nextId() throws Exception;

  void reset(long newStart) throws GidException;

  boolean adjustCache(int newCache) throws Exception;
}
