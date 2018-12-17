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

package studio.raptor.gid;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MalformedObjectNameException;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softee.management.annotation.Description;
import org.softee.management.annotation.MBean;
import org.softee.management.annotation.ManagedAttribute;
import org.softee.management.annotation.ManagedOperation;
import org.softee.management.exception.ManagementException;
import org.softee.management.helper.MBeanRegistration;
import studio.raptor.gid.common.CuratorUtil;
import studio.raptor.gid.common.GidException;
import studio.raptor.gid.common.Pair;
import studio.raptor.gid.config.ConfigLoader;
import studio.raptor.gid.def.BreadcrumbDef;
import studio.raptor.gid.def.SequenceDef;
import studio.raptor.gid.kind.Breadcrumb;
import studio.raptor.gid.kind.Sequencable;
import studio.raptor.gid.kind.Snowflake;
import studio.raptor.gid.kind.Ticktock;
import studio.raptor.gid.service.impl.CacheServiceImpl;

/**
 * <pre>
 * 序列容器
 *
 * > 提供2类序列服务
 * 1)本地计算型：snowflak ticktock 2)远程计算型 breadcrumb
 *
 * > 配置加载顺序
 * 用户定义的配置文件 > 默认配置文件sequence.xml
 *
 * > 配置序列方式
 * 1)使用配置文件 2)使用SequenceServer提供的add API进行编码新增
 *
 * > 外部依赖
 * 依赖zookeeper作为本地计算型序列的workerid,dataCenterId分配器，
 * breadcrumb依赖zookeeper生产序列值
 *
 * > ZK数据结构：
 * +[namespace]
 * |________raptor-sequences
 * |___________[seqName]
 * |________workers
 * |___________ip:port = workerId
 *
 * eg.
 * +crm
 * |________raptor-sequences
 * |___________partyId
 * |________workers
 * |___________192.168.1.110-8888 = 10
 * </pre>
 *
 * @author bruce shi
 */
@MBean(objectName = "studio.raptor.gid:name=sequencer,name=info")
@Description("sequencer")
public class Sequencer {

  private static final String DEFAULT_SEQ_DEF_FILE_PATH = "/sequence.xml";

  // --------------------------------------------------------------------
  private static Logger log = LoggerFactory.getLogger(Sequencer.class);

  // --------------------------------------------------------------------
  private String configFilePath;

  private CuratorFramework zkClient;

  private String sysId;

  private AtomicBoolean isStarted = new AtomicBoolean(false);

  private Map<String, Pair<SequenceDef, Sequencable>> sequences = new ConcurrentHashMap<String, Pair<SequenceDef, Sequencable>>();

  /**
   * 构造函数
   */
  public Sequencer(String zkConnectString, String namespace, String ip, int port)
      throws Exception {
    this(zkConnectString, namespace, null, ip, port);
  }

  /**
   * 构造函数
   */
  public Sequencer(String zkConnectString, String namespace, String sysId)
      throws Exception {
    this(zkConnectString, namespace, null, sysId);
  }

  /**
   * 构造函数
   *
   * @param zkConnectString zk连接串
   * @param namespace zk操作的命名空间
   * @param configFilePath 配置文件路径
   * @param ip 序列服务器进程绑定的IP
   * @param port 序列服务器进程绑定的端口
   */
  public Sequencer(String zkConnectString, String namespace, String configFilePath, String ip,
      int port)
      throws Exception {
    this(zkConnectString, namespace, configFilePath, ip + "-" + port);
  }

  /**
   * 构造函数
   *
   * @param zkConnectString zk连接串
   * @param namespace zk操作的命名空间
   * @param configFilePath sequence配置文件路径
   * @param sysId 工作进程的唯一标识
   */
  public Sequencer(String zkConnectString, String namespace, String configFilePath, String sysId)
      throws Exception {

    this.configFilePath = configFilePath;
    this.sysId = sysId;

    this.zkClient = CuratorUtil.newClient(zkConnectString, namespace);

    boolean isConntected = this.zkClient
        .blockUntilConnected(CuratorUtil.DEFAULT_CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

    if (isConntected) {
      createRequiredNode(this.zkClient);
    }
    //adjust cache of breadcrumb
    CacheServiceImpl.setSequenceServer(this);
  }

  //TODO v0.2 异常情况统计 + 关闭钩子

  @ManagedAttribute
  @Description("seq config file path")
  public String getConfigFilePath() {
    return configFilePath;
  }

  @ManagedAttribute
  @Description("sequencer instance id")
  public String getSysId() {
    return sysId;
  }

  @ManagedAttribute
  @Description("sequencer is started ?")
  public boolean isStarted() {
    return isStarted.get();
  }

  @ManagedAttribute
  @Description("all sequences in sequencer")
  public Map<String, Pair<SequenceDef, Sequencable>> getSequences() {
    return sequences;
  }

  /**
   * 注册MBean
   */
  public void register() throws ManagementException, MalformedObjectNameException {
    new MBeanRegistration(this).register();
  }

  /**
   * 启动序列服务器
   */
  @ManagedOperation
  @Description("start sequencer")
  public void startup() throws Exception {
    if (isStarted.compareAndSet(false, true)) {
      loadSequences(configFilePath);// 从配置文件加载seq
    } else {
      log.warn("sequencer is already started");
    }
  }

  /**
   * 不加载序列配置文件启动服务
   */
  @ManagedOperation
  @Description("start sequencer without load sequence config")
  public void startupWithoutLoad() throws Exception {
    isStarted.compareAndSet(false, true);
  }

  /**
   * 关闭序列服务器
   */
  @ManagedOperation
  @Description("stop sequencer")
  public void shutdown() {
    if (!isStarted.compareAndSet(true, false)) {
      log.warn("sequencer is already closed");
    }
  }

  @Override
  public String toString() {
    return String.format("sequencer [namespace=%s,sysId=%s]",
        zkClient.getNamespace(),
        sysId);
  }

  /**
   * 获取序列实例
   *
   * @throws GidException 序列发生器没有启动或序列实例不存在
   */
  public Sequencable get(String seqName) throws GidException {
    Preconditions.checkState(isStarted.get(), "Sequencer is NOT started");
    Pair<SequenceDef, Sequencable> seqPair = sequences.get(seqName.trim());
    if (null != seqPair) {
      Sequencable seq = seqPair.getValue();
      return seq;
    }
    throw new GidException("sequence '" + seqName + "' not exists");
  }

  /**
   * 新增序列实例
   */
  public void add(SequenceDef seqDef) throws GidException {
    Preconditions.checkState(isStarted.get(), "Sequencer is NOT started");
    Preconditions.checkNotNull(seqDef, "Sequence definition can not be null");
    Preconditions.checkNotNull(seqDef.type(), "Sequence 'name' can not be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(seqDef.name()),
        "Sequence 'name' can not be null or empty");
    Preconditions.checkState(!this.sequences.containsKey(seqDef.name().trim()),
        "The '%s' sequence already exists", seqDef.name());

    Sequencable sequence;
    switch (seqDef.type()) {
      case SNOWFLAKE:
        sequence = new Snowflake(seqDef, this.sysId, this.zkClient);
        break;
      case TICKTOCK:
        sequence = new Ticktock(seqDef, this.sysId, this.zkClient);
        break;
      case BREADCRUMB:
        Preconditions.checkArgument(((BreadcrumbDef) seqDef).incr() != 0,
            "equence 'incr' can not e 0,cur val:%s", ((BreadcrumbDef) seqDef).incr());
        Preconditions.checkArgument(((BreadcrumbDef) seqDef).cache() >= 0,
            "Sequence 'cache' must ge 0,cur val:%s", ((BreadcrumbDef) seqDef).cache());
        sequence = new Breadcrumb(seqDef, this.zkClient);
        break;
      default:
        throw new GidException("Not available sequence type : " + seqDef.type());
    }

    sequences.put(seqDef.name().trim(), new Pair<>(seqDef, sequence));
    log.info("Add sequence > {},{}", seqDef.name(), seqDef.type());
  }

  /**
   * 创建必要的ZK节点目录
   */
  private void createRequiredNode(CuratorFramework zkClient) {

    try {
      if (!CuratorUtil.checkExists(zkClient, "/workers")) {
        log.warn("'/workers' may not exists in zookeeper,now create it");
        CuratorUtil.mkDirs(zkClient, "/workers");
      }
      if (!CuratorUtil.checkExists(zkClient, "/sequences")) {
        log.warn("'/sequences' may not exists in zookeeper,now create it");
        CuratorUtil.mkDirs(zkClient, "/sequences");
      }
    } catch (Exception e) {
      e.printStackTrace();
      // ignore
    }

  }


  /**
   * 加载序列
   *
   * @param configFilePath 序列配置文件路径
   */
  private void loadSequences(String configFilePath) {
    List<SequenceDef> seqDefs = null;
    String finalConfigPath = (configFilePath == null ? DEFAULT_SEQ_DEF_FILE_PATH : configFilePath);
    try {
      seqDefs = ConfigLoader.load(finalConfigPath);
    } catch (GidException e) {
      log.warn("Load sequence define file from '{}' failure", finalConfigPath, e);
    }

    try {
      if (null == seqDefs || 0 == seqDefs.size()) {
        log.warn("Nothing to Load sequence define file from '{}'", finalConfigPath);
      } else {
        for (SequenceDef seqDef : seqDefs) {
          add(seqDef);
        }
      }
    } catch (GidException e) {
      log.warn("Load sequence define file from '{}' failure", finalConfigPath, e);
    }
  }
}
