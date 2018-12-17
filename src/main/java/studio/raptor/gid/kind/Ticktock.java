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

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.curator.framework.CuratorFramework;
import studio.raptor.gid.common.GidException;
import studio.raptor.gid.common.ThreadSafe;
import studio.raptor.gid.def.SequenceDef;

/**
 * 宏观上按时间自增的序列(snowflake变种)。
 *
 * <b>Attention</b> : 多机部署时，需要使用NTP来保障时间的同步且时钟不能向后回拨。
 * <b>Structure</b> : 12位 timestamp(yyMMddHHmmss) + 3位(default) workerid + 4位(default) seq
 * 由3个10进制构成的部分拼接而成，值对应java的long类型。
 *
 * 一、特性
 * <b>关键词：</b>全局唯一/粗略有序/时间相关/趋势递增/可制造/可反解
 * 跟snowflake类似，<b>默认情况下（序列号占4位），理论上每秒可产生10000个序列值</b>。
 *
 * 样例：1704241912300011563,1704241912300011567,1704241912310013091 ......
 *
 * 二、优缺点
 *【优】
 * 可读性高
 * 趋势递增：时间戳在高位，序列号在低位
 * 性能高无单点：本地计算不依赖数据库等第三方
 * 使用灵活：三个组成部分的位数可按需调整
 *
 *【缺】
 * 序列不连续
 * 无法控制生成规则（比如序列起始等）
 * 强依赖机器时钟，如果时钟回拨，会导致序列重复或者系统不可用
 *
 * 三、使用场景
 * 需要高性能，高可读性要求的场景，比如订单号，业务流水号等。
 *
 * @author bruce
 * @since 0.1
 *
 */
@ThreadSafe
public class Ticktock extends Snowflake {

  private static final SimpleDateFormat timestamp = new SimpleDateFormat("yyMMddHHmmss");

  /**
   * 构造函数
   *
   * @param seqDef 序列定义
   * @param sysId 系统标识
   * @param zkClient zk客户端
   */
  public Ticktock(final SequenceDef seqDef, String sysId, CuratorFramework zkClient)
      throws GidException {
    super(seqDef, sysId, zkClient);
    sequenceMask = (long) Math.pow(10, sequenceBits) - 1;
  }


  @Override
  public long timestamp() {
    return Long.parseLong(timestamp.format(new Date()));
  }


  @Override
  public long maxWorkerid() {
    return (long) Math.pow(10, workerIdBits) - 1;
  }


  @Override
  long produceSequence(long now) {
    if (++sequence > sequenceMask) {
      sequence = 0L;
      now = tilNextTime(lastTimestamp);//TODO 统计事件
    }
    return now;
  }


  @Override
  long assembleId(long now) {
    String id = String.format("%d%0" + workerIdBits + "d%0" + sequenceBits + "d",
        now,
        workerId,
        sequence);
    return Long.valueOf(id);
  }

}
