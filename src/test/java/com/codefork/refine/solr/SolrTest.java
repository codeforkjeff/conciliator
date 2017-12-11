package com.codefork.refine.solr;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.ThreadPoolFactory;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.resources.Result;
import com.codefork.refine.viaf.VIAF;
import com.codefork.refine.viaf.VIAFNameType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@WebMvcTest(Solr.class)
public class SolrTest {

    @TestConfiguration
    static class TestConfig {

        // we can't use MockBean b/c the PostConstruct hook in VIAF uses config
        // before we get a chance to put matchers on it in this test code.
        @Bean
        public Config config() {
            Properties props = new Properties();
            props.setProperty("nametype.id", "/book/book");
            props.setProperty("nametype.name", "Book");
            props.setProperty("url.query", "http://localhost:8983/solr/test-core/select?wt=xml&q={{QUERY}}&rows={{ROWS}}");
            props.setProperty("url.document", "http://localhost:8983/solr/test-core/get?id={{id}}");
            props.setProperty("field.id", "id");
            props.setProperty("field.name", "title_display");

            Config config = mock(Config.class);
            when(config.getDataSourceProperties("solr")).thenReturn(props);
            return config;
        }

        @Bean
        public ThreadPoolFactory threadPoolFactory() {
            return new ThreadPoolFactory();
        }
    }

    @MockBean
    ConnectionFactory connectionFactory;

    @Autowired
    Solr solr;

    @Autowired
    MockMvc mvc;

    @Test
    public void testSearchPersonalName() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/solr_results.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        String json = "{\"q0\":{\"query\": \"The Complete Adventures of Sherlock Holmes\",\"type\":\"/book/book\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/solr").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("The complete Sherlock Holmes", result1.get("name").asText());
        assertEquals("119390", result1.get("id").asText());
        assertEquals("0.33383894", result1.get("score").asText());

        JsonNode result2 = results.get(1);
        assertEquals("The adventures of Sherlock Holmes", result2.get("name").asText());
        assertEquals("274753", result2.get("id").asText());
        assertEquals("0.26951128", result2.get("score").asText());

        JsonNode result3 = results.get(2);
        assertEquals("The adventures of Sherlock Holmes", result3.get("name").asText());
        assertEquals("25950", result3.get("id").asText());
        assertEquals("0.2694855", result3.get("score").asText());
    }

}
