package com.codefork.refine.controllers;

import com.codefork.refine.Config;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.resources.SourceMetaDataResponse;
import com.codefork.refine.viaf.VIAF;
import com.codefork.refine.viaf.VIAFService;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReconcileControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testServiceMetaData() throws Exception {
        Config config = new Config();
        VIAF viaf = new VIAF(new VIAFService(), config);
        ReconcileController rc = new ReconcileController(viaf, config);
        ServiceMetaDataResponse response = (ServiceMetaDataResponse) rc.reconcileNoSource(null, null);
        assertEquals(response.getView().getUrl(), "http://viaf.org/viaf/{{id}}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProxyMetaData() throws Exception {
        Config config = new Config();
        VIAF viaf = new VIAF(new VIAFService(), config);
        ReconcileController rc = new ReconcileController(viaf, config);
        SourceMetaDataResponse response = (SourceMetaDataResponse) rc.reconcileProxy(null, null, "LC");
        assertEquals(response.getView().getUrl(), "http://id.loc.gov/authorities/names/{{id}}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSearch() throws Exception {
        // TODO: mock out ALL the dependencies (VIAF, Config) so we only exercise ReconcileController. I got lazy here.

        Config config = new Config();

        VIAFService viafService = mock(VIAFService.class);
        final Class testClass = getClass();
        doAnswer(new Answer<HttpURLConnection>() {
            @Override
            public HttpURLConnection answer(InvocationOnMock invocation) throws Exception {
                String arg1 = (String) invocation.getArguments()[0];
                if (arg1.contains("shakespeare")) {
                    HttpURLConnection conn = mock(HttpURLConnection.class);
                    when(conn.getInputStream()).thenReturn(testClass.getResourceAsStream("/shakespeare.xml"));
                    return conn;
                } else if(arg1.contains("wittgenstein")) {
                    HttpURLConnection conn = mock(HttpURLConnection.class);
                    when(conn.getInputStream()).thenReturn(testClass.getResourceAsStream("/wittgenstein.xml"));
                    return conn;
                }
                return null;
            }
        }).when(viafService).doSearch(anyString(), anyInt());

        String json = "{\"q0\":{\"query\": \"shakespeare\",\"type\":\"/people/person\",\"type_strict\":\"should\"},\"q1\":{\"query\":\"wittgenstein\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";
        VIAF viaf = new VIAF(new VIAFService(), config);
        ReconcileController rc = new ReconcileController(viaf, config);

        Map<String, SearchResponse> results = (Map<String, SearchResponse>) rc.reconcileNoSource(null, json);

        assertEquals(results.size(), 2);

        SearchResponse response = results.get("q0");
        List<Result> result = response.getResult();
        assertEquals(result.size(), 3);
        assertEquals(result.get(0).getId(), "96994048");
        assertEquals(result.get(0).getName(), "Shakespeare, William, 1564-1616.");
        assertEquals(result.get(0).getType().get(0).getId(), "/people/person");
        assertEquals(result.get(0).getType().get(0).getName(), "Person");
        assertEquals(String.valueOf(result.get(0).getScore()), "0.3125");
        assertEquals(result.get(0).isMatch(), false);

        SearchResponse response2 = results.get("q1");
        List<Result> result2 = response2.getResult();
        assertEquals(result2.size(), 3);
        assertEquals(result2.get(0).getId(), "24609378");
        assertEquals(result2.get(0).getName(), "Wittgenstein, Ludwig, 1889-1951");
        assertEquals(result2.get(0).getType().get(0).getId(), "/people/person");
        assertEquals(result2.get(0).getType().get(0).getName(), "Person");
        assertEquals(String.valueOf(result2.get(0).getScore()), "0.3548387096774194");
        assertEquals(result2.get(0).isMatch(), false);
    }

}