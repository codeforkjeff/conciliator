package com.codefork.refine.solr;

import com.codefork.refine.Config;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Example of a second Solr data source. You can create as many solr data sources
 * as you want, by copying/renaming this file.
 *
 * The name of this class doesn't actually matter, as long as it's unique and matches
 * the .java filename.
 *
 * @Component specifies the name that Spring uses for injection; it should match the
 * string in the @Qualifier annotation on DataSourceController.setDataSource().
 *
 * getConfigName() should return a name used for the keys in the conciliator.properties file.
 */
@Component("anothersolr")
public class AnotherSolr extends Solr {

    @Autowired
    public AnotherSolr(Config config, CacheManager cacheManager, ThreadPoolFactory threadPoolFactory, ConnectionFactory connectionFactory) {
        super(config, cacheManager, threadPoolFactory, connectionFactory);
    }

    @Override
    public String getConfigName() {
        return "anothersolr";
    }
}
