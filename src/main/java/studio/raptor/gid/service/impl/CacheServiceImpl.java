package studio.raptor.gid.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.raptor.gid.Sequencer;
import studio.raptor.gid.kind.Breadcrumb;
import studio.raptor.gid.kind.Sequencable;
import studio.raptor.gid.service.interfaces.CacheService;

/**
 * Created by dell on 2018/1/11.
 */
public class CacheServiceImpl implements CacheService{

    private static final Logger log = LoggerFactory.getLogger(CacheServiceImpl.class);
    private static Sequencer sequencer;

    public static void setSequenceServer(Sequencer sequencer){
        CacheServiceImpl.sequencer = sequencer;
    }

    @Override
    public boolean adjustCache(String name,int newCache) throws Exception{
        boolean result=false;
        Sequencable sequence=sequencer.get(name);
        if(sequence instanceof Breadcrumb){
            Breadcrumb breadcrumb=(Breadcrumb) sequence;
            result=breadcrumb.adjustCache(newCache);
        }
        return result;
    }

}
