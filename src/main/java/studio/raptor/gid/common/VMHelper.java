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

/**
 * 虚拟机相关帮助类。
 *
 * @author bruce
 * @since 0.1
 */
public class VMHelper {

  private static final String WORKER_ID = "workerid";

  /**
   * 从系统属性中获取工作节点编号
   * @return 工作节点编号
   */
  public static String getWorkerId() {
    return System.getProperty(WORKER_ID);
  }

  /**
   * 向系统属性中设置工作节点编号
   * @param workerid 工作节点编号
   */
  public static void setWorkerid(String workerid) {
    System.setProperty(WORKER_ID, workerid);
  }

  /**
   * 获取用户当前主目录
   * @return 主目录
   */
  public static String getUserHome() {
    return System.getProperty("user.home");
  }

}
