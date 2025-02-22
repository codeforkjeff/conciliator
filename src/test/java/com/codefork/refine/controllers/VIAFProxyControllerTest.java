package com.codefork.refine.controllers;

import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.datasource.MockConnectionFactoryHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public class VIAFProxyControllerTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    @Autowired
    public MockConnectionFactoryHelper mockConnectionFactoryHelper;

    @Autowired
    MockMvc mvc;

    @Test
    public void testProxyMetaData() throws Exception {
        MvcResult result = mvc.perform(get("/reconcile/viafproxy/LC")).andReturn();

        String body = result.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals("LC (by way of VIAF)", root.get("name").asText());
        assertEquals("http://id.loc.gov/authorities/names/{{id}}", root.get("view").get("url").asText());
    }

    @Test
    public void testSearchProxyModeLC() throws Exception {

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22%20and%20local.sources%20%3D%20%22lc%22&sortKeys=holdingscount&maximumRecords=3",
                "/shakespeare_lc.xml");

        String json = "{\"q0\":{\"query\": \"Shakespeare, William, 1564-1616.\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/LC").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("n78095332", result1.get("id").asText());
        assertTrue(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Hauptmann, Gerhart, 1862-1946.", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("n80076391", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Pasternak, Boris Leonidovich, 1890-1960.", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("n79018438", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    /**
     * VIAF gives a URL for the BNF source record ID. this test
     * checks that we use the ID parsed out of the "sid" XML element instead.
     */
    @Test
    public void testSearchProxyModeBNF() throws Exception {

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22%20and%20local.sources%20%3D%20%22bnf%22&sortKeys=holdingscount&maximumRecords=3",
                "/shakespeare_bnf.xml");

        String json = "{\"q0\":{\"query\": \"Shakespeare, William, 1564-1616.\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/BNF").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("11924607", result1.get("id").asText());
        assertTrue(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Hauptmann, Gerhart, 1862-1946.", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("12026662", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Pasternak, Boris Leonidovič, 1890-1960", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("11918737", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    /**
     * Test case for XML missing an ID in ../mainHeadings/data/sources
     * but having an ID under ../VIAFCluster/sources.
     */
    @Test
    public void testSearchProxyModeBNFMissingID() throws Exception {

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Jean-Fran%C3%A7ois%20Alexandre%201804%201874%22%20and%20local.sources%20%3D%20%22bnf%22&sortKeys=holdingscount&maximumRecords=3",
                "/alexandre.xml");

        String json = "{\"q0\":{\"query\": \"Jean-François Alexandre 1804 1874\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/BNF").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        // this entry in XML is missing an ID ../mainHeadings/data/sources
        JsonNode result1 = results.get(0);
        assertEquals("Alexandre, Jean-François 1804-1874", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("10341017", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Blanc, François 166.-1742", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("10343440", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Arago, Jacques, 1790-1855", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("12265696", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    /**
     * Test case for URL in source ID mapping, which formatResult() should treat
     * as special case
     */
    @Test
    public void testSearchProxyModeDNB() throws Exception {

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22hegel%22%20and%20local.sources%20%3D%20%22dnb%22&sortKeys=holdingscount&maximumRecords=3",
                "/shakespeare_dnb.xml");

        String json = "{\"q0\":{\"query\": \"hegel\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/DNB").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Hegel, Georg Wilhelm Friedrich, 1770-1831.", result1.get("name").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("118547739", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Koyré, Alexandre, 1892-1964", result2.get("name").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("118777890", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Friedrich, Carl J. 1901-1984", result3.get("name").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("118535870", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    /**
     * VIAFProxyController doesn't (currently) inherit from DataSourceController so make sure CORS works for it
     */
    @Test
    public void testCors() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/reconcile/viafproxy/LC").header("Origin", "http://testo.com")).andReturn();

        assertEquals("*", mvcResult.getResponse().getHeaderValue("Access-Control-Allow-Origin"));
    }

}
