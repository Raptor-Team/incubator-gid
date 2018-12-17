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
 * Snowflake序列定义。
 *
 * @author bruce
 * @since 0.1
 */
public abstract class SnowflakeDef implements SequenceDef {

  public SnowflakeDef() throws GidException {
    validate();
  }

  public abstract String name();

  @Override
  public Type type() {
    return Type.SNOWFLAKE;
  }

  public abstract int workerIdWidth();

  public abstract int sequenceWidth();

  @Override
  public void validate() throws GidException {
    // 工作节点编码位数 + 序列号位数 必须 等于 22 ， 为了控制整体长度在64位(bit)
    if (workerIdWidth() + sequenceWidth() != 22) {
      throw new GidException(
          name() + " -> (workerIdWidth+sequenceWidth)@snowflakeDef must be equal 22");
    }
  }

  @Override
  public String toString(){
    return String.format("sequenceDef[name=%s" + ",type=%s" + ",workerIdWidth=%s" + ",sequenceWidth=%s]",
        name(),
        type().name,
        workerIdWidth(), sequenceWidth());
  }

}
