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

import java.util.LinkedList;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zookeeper操作工具类。
 *
 * @author bruce
 * @since 0.1
 */
public class CuratorUtil {

  public static final int DEFAULT_SESSION_TIMEOUT_MS = 10000; // 默认会话超时ms
  public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 10000; // 默认连接超时ms
  private static Logger log = LoggerFactory.getLogger(CuratorUtil.class);

  /**
   * 创建zookeeper客户端
   *
   * @param connectString 连接串 ip1:port1,ip2:port2......
   * @param namespace 命名空间
   * @return zk客户端对象
   */
  public static CuratorFramework newClient(String connectString, String namespace) {
    return newClient(connectString, DEFAULT_SESSION_TIMEOUT_MS, DEFAULT_CONNECTION_TIMEOUT_MS,
        namespace, null);
  }

  /**
   * 创建zookeeper客户端
   *
   * @param connectString 连接串 ip1:port1,ip2:port2......
   * @param namespace 命名空间
   * @param listener 监听器
   * @return zk客户端对象
   */
  public static CuratorFramework newClient(String connectString, String namespace,
      ConnectionStateListener listener) {
    return newClient(connectString, DEFAULT_SESSION_TIMEOUT_MS, DEFAULT_CONNECTION_TIMEOUT_MS,
        namespace, listener);
  }

  /**
   * 创建zookeeper客户端
   *
   * @param connectString 连接串 ip1:port1,ip2:port2......
   * @param sessionTimeout 会话超时时间（毫秒）
   * @param connectTimeout 连接超时时间（毫秒）
   * @param namespace 命名空间
   * @param listener 监听器
   * @return zk客户端对象
   */
  public static CuratorFramework newClient(String connectString, int sessionTimeout,
      int connectTimeout,
      String namespace, ConnectionStateListener listener) {
    CuratorFramework client = CuratorFrameworkFactory.builder()
        .connectString(connectString)
        .sessionTimeoutMs(sessionTimeout)
        .connectionTimeoutMs(connectTimeout)
        .namespace(namespace)
        .retryPolicy(new RetryNTimes(3, 1000))
        .build();

    if (client.getState() == CuratorFrameworkState.LATENT) {
      client.start();
    }

    if (null != listener) {
      client.getConnectionStateListenable().addListener(listener);
    }

    return client;
  }

  /**
   * 销毁zookeeper客户端
   *
   * @param client zk客户端
   */
  public static void destory(CuratorFramework client) {
    if (null != client) {
      CloseableUtils.closeQuietly(client);
    } else {
      log.warn("zookeeper zkClient is null");
    }
  }

  /**
   * 检查路径是否存在
   *
   * @param client zk客户端
   * @param path 路径
   * @return 存在返回true,反之返回false
   * @throws Exception zk errors
   */
  public static boolean checkExists(CuratorFramework client, String path) throws Exception {
    Stat stat = client.checkExists().forPath(path);
    if (null != stat) {// path exists
      return true;
    } else {
      return false;
    }
  }

  /**
   * 创建目录（支持多级目录）
   *
   * @param client zk客户端
   * @param path 路径
   * @throws Exception zk errors
   */
  public static void mkDirs(CuratorFramework client, String path) throws Exception {
    client.create().creatingParentsIfNeeded().forPath(path);
  }

  /**
   * 删除目录（包括子目录或节点）
   *
   * @param client zk客户端
   * @param path 路径
   * @throws Exception zk errors
   */
  public static void delDirs(CuratorFramework client, String path) throws Exception {
    client.delete().deletingChildrenIfNeeded().forPath(path);
  }

  /**
   * 创建持久节点（未创建的目录自动完成创建）
   *
   * @param client zk客户端
   * @param path 路径
   * @param data 数据
   * @throws Exception zk errors
   */
  public static void createPersistentNode(CuratorFramework client, String path, byte[] data)
      throws Exception {
    client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path, data);
  }

  /**
   * 创建瞬时节点（未创建的目录自动完成创建）
   *
   * @param client zk客户端
   * @param path 路径
   * @param data 数据
   * @throws Exception zk errors
   */
  public static void createEphemeralNode(CuratorFramework client, String path, byte[] data)
      throws Exception {
    client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, data);
  }

  /**
   * 删除节点
   *
   * @param client zk客户端
   * @param path 路径
   * @throws Exception zk errors
   */
  public static void deleteNode(CuratorFramework client, String path) throws Exception {
    client.delete().forPath(path);
  }

  /**
   * 在指定路径上设置数据
   *
   * @param client zk客户端
   * @param path 路径
   * @param data 数据
   * @throws Exception zk errors
   */
  public static void setData(CuratorFramework client, String path, byte[] data) throws Exception {
    client.setData().forPath(path, data);
  }

  /**
   * 获取指定路径上的数据
   *
   * @param client zk客户端
   * @param path 路径
   * @return 路径上的数据
   * @throws Exception zk errors
   */
  public static byte[] getData(CuratorFramework client, String path) throws Exception {
    return client.getData().forPath(path);
  }

  /**
   * 获取目录子节点列表
   *
   * @param client zk客户端
   * @param path 路径
   * @param isFullPath 是否输出完整路径
   * @return 子节点列表
   * @throws Exception zk errors
   */

  public static List<String> getChildren(CuratorFramework client, String path, boolean isFullPath)
      throws Exception {
    if (isFullPath) {
      List<String> paths = client.getChildren().forPath(path);
      List<String> fullPaths = new LinkedList<String>();
      for (String children : paths) {
        children = ZKPaths.makePath(ZKPaths.makePath(client.getNamespace(), path), children);
        fullPaths.add(children);
      }
      return fullPaths;
    }

    return client.getChildren().forPath(path);
  }

}
