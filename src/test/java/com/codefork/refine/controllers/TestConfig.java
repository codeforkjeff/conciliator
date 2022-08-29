package com.codefork.refine.controllers;

import com.codefork.refine.Config;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Properties;

/*
 * TODO: This only provides a single Config bean for the whole test environment;
 * ideally, we'd want specialized Configs for different tests or groups of tests
 * so this needs some reworking.
 */
@Component
@Profile("test")
public class TestConfig extends Config {
    public static final int TTL_SECONDS = 1;

    public TestConfig() {
        super();
        Properties props = new Properties();
        props.setProperty(Config.PROP_CACHE_TTL, String.valueOf(TTL_SECONDS));

        props.setProperty("datasource.solr.nametype.id", "/book/book");
        props.setProperty("datasource.solr.nametype.name", "Book");
        props.setProperty("datasource.solr.url.query", "http://localhost:8983/solr/test-core/select?wt=xml&q={{QUERY}}&rows={{ROWS}}");
        props.setProperty("datasource.solr.url.document", "http://localhost:8983/solr/test-core/get?id={{id}}");
        props.setProperty("datasource.solr.field.id", "id");
        props.setProperty("datasource.solr.field.name", "title_display");

        merge(props);
    }

}
