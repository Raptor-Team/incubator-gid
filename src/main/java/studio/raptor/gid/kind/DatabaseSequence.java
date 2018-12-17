package studio.raptor.gid.kind;

import org.apache.curator.framework.CuratorFramework;

/**
 * 基于数据库的序列
 *
 * @author bruce
 * @since 0.1
 */
public abstract class DatabaseSequence implements Sequencable {

  public DatabaseSequence(CuratorFramework zkClient) {

  }

}
