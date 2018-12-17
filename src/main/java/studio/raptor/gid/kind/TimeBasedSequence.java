package studio.raptor.gid.kind;

import org.apache.curator.framework.CuratorFramework;
import studio.raptor.gid.common.GidException;

/**
 * 基于时间戳的远程序列
 *
 * @author bruce
 */
@Deprecated
public abstract class TimeBasedSequence extends ZookeeperSequence {

  public TimeBasedSequence(CuratorFramework zkClient) throws GidException {
    super(zkClient);
  }

  /**
   * 获取当前时间戳
   *
   * @return 时间戳
   */
  abstract long timestamp();


}
