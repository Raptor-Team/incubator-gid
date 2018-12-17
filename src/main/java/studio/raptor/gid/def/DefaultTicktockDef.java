package studio.raptor.gid.def;


import studio.raptor.gid.common.GidException;

/**
 * 默认的Ticktock定义。
 *
 * @author bruce
 * @since 0.1
 */
public abstract class DefaultTicktockDef extends TicktockDef {

  public static final int DEFAULT_WORKERID_BITS = 3; // 默认工作节点ID位数

  public static final int DEFAULT_SEQUENCE_BITS = 4; // 默认序列位数

  public DefaultTicktockDef() throws GidException {
    super();
  }

  @Override
  public int workerIdWidth() {
    return DEFAULT_WORKERID_BITS;
  }

  @Override
  public int sequenceWidth() {
    return DEFAULT_SEQUENCE_BITS;
  }

}
