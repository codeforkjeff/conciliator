package com.codefork.refine.controllers;

import com.codefork.refine.Config;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.orcid.Orcid;
import com.codefork.refine.orcid.OrcidMetaDataResponse;
import com.codefork.refine.resources.Result;
import com.codefork.refine.resources.SearchResponse;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import com.codefork.refine.viaf.VIAFProxyModeMetaDataResponse;
import com.codefork.refine.viaf.VIAF;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
        VIAF viaf = new VIAF();
        viaf.init(config);
        ReconcileController rc = new ReconcileController(config);
        ServiceMetaDataResponse response = (ServiceMetaDataResponse) rc.reconcile(viaf, null, null, Collections.EMPTY_MAP);
        assertEquals(response.getName(), "VIAF");
        assertEquals(response.getView().getUrl(), "http://viaf.org/viaf/{{id}}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProxyMetaData() throws Exception {
        Config config = new Config();
        VIAF viaf = new VIAF();
        viaf.init(config);
        ReconcileController rc = new ReconcileController(config);

        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(VIAF.EXTRA_PARAM_SOURCE_FROM_PATH, "LC");
        extraParams.put(VIAF.EXTRA_PARAM_PROXY_MODE, "true");

        VIAFProxyModeMetaDataResponse response = (VIAFProxyModeMetaDataResponse) rc.reconcile(viaf, null, null, extraParams);
        assertEquals(response.getName(), "LC (by way of VIAF)");
        assertEquals(response.getView().getUrl(), "http://id.loc.gov/authorities/names/{{id}}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOrcidMetaData() throws Exception {
        Config config = new Config();
        ReconcileController rc = new ReconcileController(config);

        HttpServletRequest request  = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/reconcile/orcid");

        OrcidMetaDataResponse response = (OrcidMetaDataResponse) rc.reconcile(request, null, null);

        assertEquals(response.getName(), "ORCID");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSearch() throws Exception {
        // TODO: mock out ALL the dependencies (VIAF, Config) so we only exercise ReconcileController. I got lazy here.

        Config config = new Config();

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
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
        }).when(connectionFactory).createConnection(anyString());

        String json = "{\"q0\":{\"query\": \"shakespeare\",\"type\":\"/people/person\",\"type_strict\":\"should\"},\"q1\":{\"query\":\"wittgenstein\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";
        ReconcileController rc = new ReconcileController(config);

        HttpServletRequest request  = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/reconcile/viaf");

        Map<String, SearchResponse> results = (Map<String, SearchResponse>) rc.reconcile(request, null, json);

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

    public void testMissingDataSource() throws Exception {
        Config config = new Config();
        ReconcileController rc = new ReconcileController(config);

        HttpServletRequest request  = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/reconcile/nonexistingdatasource");

        ResponseEntity response = (ResponseEntity) rc.reconcile(request, null, null);
        assertEquals(404, response.getStatusCode().value());
    }

}