package com.codefork.refine.controllers;

import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.viaf.VIAF;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
public class VIAFTooManyRequestsTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ConnectionFactory connectionFactory() {
            return new ConnectionFactory () {

                @Override
                public HttpURLConnection createConnection(String url) throws IOException {
                    HttpURLConnection conn = mock(HttpURLConnection.class);

                    String error = "Server returned HTTP response code: 429 for URL:" + url;
                    when(conn.getInputStream()).thenThrow(new IOException(error));
                    return conn;
                }
            };
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    VIAF viaf;

    // test that getting an http response 429 ("too many requests") code
    // results in shrinking the thread pool
    @Test
    public void testTooManyRequestsResponse() throws Exception {

        int startSize = viaf.getThreadPool().getPoolSize();

        String json = "{\"q0\":{\"query\": \"whatever\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(0, results.size());

        assertEquals(startSize - 1, viaf.getThreadPool().getPoolSize());
    }

}
