package com.codefork.refine.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Component
public class MockConnectionFactoryHelper {

    public HttpURLConnection createMockHttpURLConnection(String resource) throws IOException {
        Log log = LogFactory.getLog(getClass());
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream is = getClass().getResourceAsStream(resource);

        if(is != null) {
            when(conn.getInputStream()).thenReturn(is);
        } else {
            String msg = "Resource file not found: " + resource;
            log.error(msg);
            throw new IOException(msg);
        }
        return conn;
    }

    public OngoingStubbing<HttpURLConnection> expect(ConnectionFactory connectionFactory, String url, String resource, int times) throws Exception {
        // prevent UnfinishedStubbingException exceptions but creating these mocks first
        List<HttpURLConnection> connections = new ArrayList<HttpURLConnection>();
        for(int i = 0; i < times; i++) {
            connections.add(createMockHttpURLConnection(resource));
        }
        OngoingStubbing<HttpURLConnection> stub = Mockito.when(connectionFactory.createConnection(url));
        for(int i = 0; i < times; i++) {
            stub = stub.thenReturn(connections.get(i));
        }
        return stub;
    }

    public OngoingStubbing<HttpURLConnection> expect(ConnectionFactory connectionFactory, String url, String resource) throws Exception {
        return expect(connectionFactory, url, resource, 1);
    }

}
