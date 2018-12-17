package studio.raptor.gid.service.interfaces;

/**
 * @author Created by liujy on 2018/1/11.
 * Breadcrumb 类型序列本地缓存动态调整
 */
public interface CacheService {

    /**
     * 调整 单个Breadcrumb 序列的本地cache值,非立即生效
     * cache调整时,等待上一个idPool结束，才开始下一个新size的idPool
     * @param name
     * @param newCache
     * @return
     */
    boolean adjustCache(String name,int newCache) throws Exception;
}
