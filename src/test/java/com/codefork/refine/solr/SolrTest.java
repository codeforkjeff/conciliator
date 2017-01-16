package com.codefork.refine.solr;

import com.codefork.refine.Config;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.resources.Result;
import com.codefork.refine.viaf.VIAF;
import com.codefork.refine.viaf.VIAFNameType;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SolrTest {

    ConnectionFactory connectionFactory;
    Config config;
    Solr solr;

    @Test
    public void testSearchPersonalName() throws Exception {
        connectionFactory = mock(ConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream("/solr_results.xml");
        when(connectionFactory.createConnection(anyString())).thenReturn(conn);
        when(conn.getInputStream()).thenReturn(is);

        Properties props = new Properties();
        props.setProperty("nametype.id", "/book/book");
        props.setProperty("nametype.name", "Book");
        props.setProperty("url.query", "http://localhost:8983/solr/test-core/select?wt=xml&q={{QUERY}}&rows={{ROWS}}");
        props.setProperty("url.document", "http://localhost:8983/solr/test-core/get?id={{id}}");
        props.setProperty("field.id", "id");
        props.setProperty("field.name", "title_display");

        config = mock(Config.class);
        when(config.getDataSourceProperties("solr")).thenReturn(props);

        solr = new Solr();
        solr.setConfigName("solr");
        solr.setConnectionFactory(connectionFactory);
        solr.init(config);

        SearchQuery query = new SearchQuery("The Complete Adventures of Sherlock Holmes", 3, VIAFNameType.Book.asNameType(), "should");
        List<Result> results = solr.searchCheckCache(query);

        assertEquals(3, results.size());

        Result result1 = results.get(0);
        assertEquals("119390", result1.getId());
        assertEquals("The complete Sherlock Holmes", result1.getName());
        assertEquals("0.33383894", String.valueOf(result1.getScore()));

        Result result2 = results.get(1);
        assertEquals("274753", result2.getId());
        assertEquals("The adventures of Sherlock Holmes", result2.getName());
        assertEquals("0.26951128", String.valueOf(result2.getScore()));

        Result result3 = results.get(2);
        assertEquals("25950", result3.getId());
        assertEquals("The adventures of Sherlock Holmes", result3.getName());
        assertEquals("0.2694855", String.valueOf(result3.getScore()));
    }

}
