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

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.raptor.gid.common.CuratorUtil;
import studio.raptor.gid.common.GidException;
import studio.raptor.gid.common.ThreadSafe;
import studio.raptor.gid.common.VMHelper;
import studio.raptor.gid.def.SequenceDef;
import studio.raptor.gid.def.SnowflakeDef;

/**
 * <pre>
 * 宏观上按时间自增的序列。
 *
 * <b>Attention</b> : 多机部署时，需要使用NTP来保障时间的同步且时钟不能向后回拨。
 * <b>Structure</b> : 42bit timestamp(1bit avaliable) | 10bit(default) workerid | 12bit(default)seq
 *
 * 一、特性
 * <b>关键词：</b>全局唯一/粗略有序/时间相关/趋势递增/可制造/可反解
 * 基于”时间戳+节点编号+序列号“共计64bit组合而成的序列，可以保证全局唯一和宏观自增，不依赖于数据库等组件，仅本地计算即可，性能非常高；默认情况下（序列号占12bit），理论上每秒可产生4096000个序列值。
 * 注意：序列值对应java的long类型，数据库的BIGINT类型。
 * 样例：576982432306171904,576982432310366208,576982432310366209 ......
 *
 *
 * 二、优缺点
 * 【优】
 * 趋势递增：毫秒数在高位，序列号在低位
 * 性能高无单点：本地计算不依赖数据库等第三方
 * 使用灵活：三个组成部分的位数可按需调整
 *
 * 【缺】
 * 人工不可读
 * 序列不连续
 * 无法控制生成规则（比如序列起始等）
 * 强依赖机器时钟，如果时钟回拨，会导致序列重复或者系统不可用
 *
 * 三、使用场景
 * 需要高性能而又对序列值的可读性要求不高的场景，比如性能跟踪用的traceid、日志编号、消息编号等。
 *
 * </pre>
 *
 * @author bruce
 * @since 0.1
 */
@ThreadSafe
public class Snowflake extends ZookeeperSequence {

  public static final Logger log = LoggerFactory.getLogger(Snowflake.class);

  /**
   * 工作节点路径@zk
   */
  public static final String WK_ROOT_PATH = "/workers/";

  /**
   * 基准时间
   */
  private final long twepoch = 1355285532520L;

  /**
   * 系统标识 用于识别系统进程
   */
  String sysId;

  /**
   * 工作节点编号
   */
  int workerId;

  /**
   * 序列位数
   */
  int sequenceBits;

  /**
   * 工作节点位数
   */
  int workerIdBits;

  /**
   * 工作节点偏移
   */
  long workerIdShift;

  /**
   * 时间戳偏移
   */
  long timestampShift;


  long lastTimestamp = -1L;

  long sequenceMask;

  volatile long sequence = 0L;


  /**
   * 构造函数
   *
   * @param seqDef 序列定义
   * @param sysId 系统标识
   * @param zkClient zk客户端
   * @throws GidException 校验异常
   */
  public Snowflake(final SequenceDef seqDef, String sysId, CuratorFramework zkClient)
      throws GidException {

    super(zkClient);

    this.workerIdBits = ((SnowflakeDef) seqDef).workerIdWidth();
    this.sequenceBits = ((SnowflakeDef) seqDef).sequenceWidth();
    this.workerIdShift = this.sequenceBits;
    this.timestampShift = sequenceBits + workerIdBits;
    this.sequenceMask = -1L ^ -1L << this.sequenceBits;

    this.sysId = sysId;

    this.workerId = workerid();
    if (this.workerId > maxWorkerid() || this.workerId < 0) {
      throw new GidException(
          "workerid can't be greater than " + maxWorkerid() + " or less than 0");
    }
  }

  /**
   * 最大工作节点编号
   *
   * @return 最大工作节点编号
   */
  public long maxWorkerid() {
    return -1L ^ -1L << workerIdBits;
  }

  /**
   * 时间戳
   *
   * @return 当前时间戳
   */
  long timestamp() {
    return System.currentTimeMillis();
  }


  @Override
  public long nextId() throws GidException {
    return nextId0();
  }

  synchronized long nextId0() throws GidException {
    long now = this.timestamp();

    // 系统时钟不可用
    if (now < this.lastTimestamp) {
      throw new GidException("Clock moved backwards.  Refusing to generate id for "
          + (this.lastTimestamp - now) + " milliseconds");
    }
    // 同一时刻
    if (this.lastTimestamp == now) {
      //序列值超出最大值，阻塞到下一个时刻
      now = produceSequence(now);
    } else {
      this.sequence = 0L;
    }

    this.lastTimestamp = now;
    return assembleId(now);
  }

  /**
   * 生产序列
   *
   * @param now 当前时间戳
   * @return 序列值
   */
  long produceSequence(long now) {
    this.sequence = (this.sequence + 1) & this.sequenceMask;
    if (this.sequence == 0) {
      now = this.tilNextTime(this.lastTimestamp);
    }
    return now;
  }

  /**
   * 组装完整的ID
   *
   * @param now 当前时间戳
   * @return 完整ID
   */
  long assembleId(long now) {
    return (now - this.twepoch) << this.timestampShift | workerId << this.workerIdShift
        | this.sequence;
  }

  /**
   * 延迟到下一个时间点
   *
   * @param lastTimestamp 上一次的时间戳
   * @return 当前时间戳
   */
  long tilNextTime(long lastTimestamp) {
    long now = this.timestamp();
    while (now <= lastTimestamp) {
      now = this.timestamp();
    }
    return now;
  }

  /**
   * 获取工作节点编号
   *
   * @return 工作节点编号
   * @throws GidException 无法获取工作节点编号等
   */
  private int workerid() throws GidException {
    // 从VM获取
    if (VMHelper.getWorkerId() != null) {
      return Integer.parseInt(VMHelper.getWorkerId());
    }
    log.warn("Skip over getting workerid from VM.");

    // 从ZK获取
    if (this.isConnected) {
      try {
        byte[] workerId = CuratorUtil.getData(this.zkClient, WK_ROOT_PATH + this.sysId);
        if (workerId.length != 0) {
          persistToFile(new String(workerId), this.sysId);
          return Integer.parseInt(new String(workerId));
        }
      } catch (Exception e) {
        e.printStackTrace();
        // igore
      }
    }
    log.warn("Skip over getting workerid from ZOOKEEPER");

    // 从本地文件
    String workeridStr = null;
    try {
      workeridStr = readWorkeridFromFile(this.sysId);
    } catch (IOException e) {
      e.printStackTrace();
      // igore
    }
    log.warn("Skip over getting workerid from FILE");

    if (Strings.isNullOrEmpty(workeridStr)) {
      throw new GidException("Workerid from vm/zk/file is null or empty ");
    } else {
      return Integer.valueOf(workeridStr);
    }
  }


  /**
   * 根据系统进程标识从文件中获取工作节点编号
   *
   * @param sysId 进程标识
   * @return 工作节点编号
   * @throws IOException 文件操作异常（文件不存在，不可读等）
   */
  private String readWorkeridFromFile(String sysId) throws IOException {
    String path = VMHelper.getUserHome() + File.separator + sysId;
    return Files.readFirstLine(new File(path), Charsets.UTF_8);
  }

  /**
   * 持久化工作节点编号到文件
   *
   * @param workerid 工作节点编号
   * @param sysId 进程标识
   * @throws IOException 文件操作异常（文件不存在，不可读等）
   */
  private void persistToFile(String workerid, String sysId) throws IOException {
    String path = VMHelper.getUserHome() + File.separator + sysId;
    Files.write(workerid, new File(path), Charsets.UTF_8);
  }

}
