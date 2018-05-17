package com.codefork.refine.solr;

import com.codefork.refine.controllers.DataSourceController;
import com.codefork.refine.datasource.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Example of a second Solr URL end point. You can create as many solr controllers
 * as you want, by copying/renaming this file.
 *
 * The name of this class doesn't actually matter, as long as it's unique and matches
 * the .java filename.
 *
 * Change @RequestMapping to the URL path you want to use. This is the path you'll type into
 * Open Refine when adding a new service.
 *
 * Change @Qualifier on setDataSource() to the name in your data source's @Component annotation.
 */
@Controller
@RequestMapping("/reconcile/anothersolr")
public class AnotherSolrController extends DataSourceController {

    @Autowired
    @Qualifier("anothersolr")
    @Override
    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);
    }

}
