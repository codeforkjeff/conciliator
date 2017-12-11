package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.resources.Result;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class VIAFLiveTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ConnectionFactory connectionFactory() {
            return new ConnectionFactory();
        }

        @Bean
        public VIAF viaf() {
            return new VIAF();
        }

        // we can't use @MockBean b/c the PostConstruct hook in VIAF uses config
        // before we get a chance to stub out calls.
        @Bean
        public Config config() {
            Config config = mock(Config.class);
            when(config.getDataSourceProperties("viaf")).thenReturn(new Properties());
            return config;
        }
    }

    @Autowired
    VIAF viaf;

    /**
     * Simple test for parsing live VIAF XML
     */
    @Test
    public void testLiveSearch() throws Exception {
        SearchQuery query = new SearchQuery("shakespeare", 3, null, "should");
        List<Result> results = viaf.searchCheckCache(query);

        // first result for shakespeare shouldn't ever change;
        // it makes for a fairly stable test

        assertEquals(3, results.size());

        Result result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.getName());
        assertEquals(VIAFNameType.Person.asNameType(), result1.getType().get(0));
        assertEquals("96994048", result1.getId());
        assertFalse(result1.isMatch());
    }
}
