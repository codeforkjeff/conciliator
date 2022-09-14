package com.codefork.refine.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public class VIAFSourceSpecificControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    public void testProxyMetaData() throws Exception {
        MvcResult result = mvc.perform(get("/reconcile/viaf/LC")).andReturn();

        String body = result.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals("VIAF - LC", root.get("name").asText());
        assertEquals("https://viaf.org/viaf/{{id}}", root.get("view").get("url").asText());
    }

    /**
     * VIAFSourceSpecificController doesn't (currently) inherit from DataSourceController so make sure CORS works for it
     */
    @Test
    public void testCors() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf/LC").header("Origin", "http://testo.com")).andReturn();

        assertEquals("*", mvcResult.getResponse().getHeaderValue("Access-Control-Allow-Origin"));
    }
}
