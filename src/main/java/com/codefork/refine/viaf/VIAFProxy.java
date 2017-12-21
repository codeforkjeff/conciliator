package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

/**
 * VIAF proxy data source
 */
public class VIAFProxy extends VIAFBase {

    @Autowired
    public VIAFProxy(Config config, CacheManager cacheManager, ThreadPoolFactory threadPoolFactory, ConnectionFactory connectionFactory) {
        super(config, cacheManager, threadPoolFactory, connectionFactory);
    }
}
