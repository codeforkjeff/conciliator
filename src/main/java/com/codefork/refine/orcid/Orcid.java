package com.codefork.refine.orcid;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.resources.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("orcid")
public class Orcid extends OrcidBase {

    @Autowired
    public Orcid(Config config, CacheManager cacheManager, ThreadPoolFactory threadPoolFactory, ConnectionFactory connectionFactory) {
        super(config, cacheManager, threadPoolFactory, connectionFactory);
    }

    @Override
    public List<Result> search(SearchQuery query) throws Exception {
        List<Result> results = searchKeyword(query);
        return results;
    }

}
