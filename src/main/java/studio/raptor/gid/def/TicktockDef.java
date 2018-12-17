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
 * 时钟型序列实例定义。
 *
 * @author bruce
 * @since 0.1
 */
public abstract class TicktockDef extends SnowflakeDef {

  public TicktockDef() throws GidException {
    super();
  }

  @Override
  public Type type() {
    return Type.TICKTOCK;
  }

  @Override
  public void validate() throws GidException {
    // 工作节点编码位数 + 序列号位数 必须 等于 7 ， 为了控制整体长度在64位(bit)
    if (workerIdWidth() + sequenceWidth() != 7) {
      throw new GidException(
          name() + " -> (workerIdWidth+sequenceWidth)@ticktockDef must be equal 7");
    }
  }

}
