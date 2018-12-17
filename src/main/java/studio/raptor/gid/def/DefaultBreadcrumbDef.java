package studio.raptor.gid.def;

import studio.raptor.gid.common.GidException;

/**
 * 默认breadcrumb序列定义。
 *
 * @author bruce
 * @since 0.1
 */
public abstract class DefaultBreadcrumbDef extends BreadcrumbDef {

  public static final int DEFAULT_CACHE = 0; // 默认缓冲大小

  public static final long DEFAULT_INCR = 1; // 默认增长步长

  //start 在序列已使用过后无效
  public static final long DEFAULT_START = 0; // 默认起始位置

  public DefaultBreadcrumbDef() throws GidException {
    super();
  }

  @Override
  public int cache() {
    return DEFAULT_CACHE;
  }

  @Override
  public long incr() {
    return DEFAULT_INCR;
  }

  @Override
  public long start() {
    return DEFAULT_START;
  }
}
