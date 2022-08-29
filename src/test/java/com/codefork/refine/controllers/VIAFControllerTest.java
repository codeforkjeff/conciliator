package com.codefork.refine.controllers;

import com.codefork.refine.Application;
import com.codefork.refine.Config;
import com.codefork.refine.datasource.ConnectionFactory;
import com.codefork.refine.datasource.MockConnectionFactoryHelper;
import com.codefork.refine.viaf.VIAF;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class VIAFControllerTest {
    public static final int TTL_SECONDS = 1;

    @MockBean
    private ConnectionFactory connectionFactory;

    public MockConnectionFactoryHelper mockConnectionFactoryHelper = new MockConnectionFactoryHelper();

    @Autowired
    public Config config;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    VIAF viaf;

    @Autowired
    MockMvc mvc;

    @Test
    public void testServiceMetadata() throws Exception {
        MvcResult result = mvc.perform(get("/reconcile/viaf")).andReturn();

        String body = result.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals("VIAF", root.get("name").asText());
        assertEquals("https://viaf.org/viaf/{{id}}", root.get("view").get("url").asText());
    }

    @Test
    public void testSearchMultiple() throws Exception {
        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22wittgenstein%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/wittgenstein_personalnames.xml");
        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22shakespeare%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/shakespeare.xml");

        String json = "{\"q0\":{\"query\": \"shakespeare\",\"type\":\"/people/person\",\"type_strict\":\"should\"},\"q1\":{\"query\":\"wittgenstein\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        assertEquals(2, root.size());

        assertEquals(3, root.get("q0").get("result").size());
        JsonNode result = root.get("q0").get("result").get(0);
        assertEquals("96994048", result.get("id").asText());
        assertEquals("Shakespeare, William, 1564-1616.", result.get("name").asText());
        assertEquals("/people/person", result.get("type").get(0).get("id").asText());
        assertEquals("Person", result.get("type").get(0).get("name").asText());
        assertEquals("0.3125", result.get("score").asText());
        assertFalse(result.get("match").asBoolean());

        assertEquals(3, root.get("q1").get("result").size());
        JsonNode result2 = root.get("q1").get("result").get(0);

        assertEquals("24609378", result2.get("id").asText());
        assertEquals("Wittgenstein, Ludwig, 1889-1951.", result2.get("name").asText());
        assertEquals("/people/person", result2.get("type").get(0).get("id").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("0.34375", result2.get("score").asText());
        assertFalse(result2.get("match").asBoolean());
    }

    public JsonNode doSearchSingle(String queryValue) throws Exception {
        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("query", queryValue)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        return new ObjectMapper().readTree(body);
    }

    @Test
    public void testSearchSingleWithText() throws Exception {
        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22wittgenstein%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/wittgenstein.xml");

        JsonNode root = doSearchSingle("wittgenstein");

        JsonNode results = root.get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Wittgenstein, Ludwig, 1889-1951", result1.get("name").asText());
        assertEquals("/people/person", result1.get("type").get(0).get("id").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("24609378", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Klossowski, Pierre, 1905-2001", result2.get("name").asText());
        assertEquals("/people/person", result2.get("type").get(0).get("id").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("27066848", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Anscombe, G. E. M. (Gertrude Elizabeth Margaret)", result3.get("name").asText());
        assertEquals("/people/person", result3.get("type").get(0).get("id").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("59078032", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchSingleWithJson() throws Exception {

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22wittgenstein%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/wittgenstein_personalnames.xml");

        String json = "{\"query\": \"wittgenstein\",\"type\":\"/people/person\",\"type_strict\":\"should\"}";
        JsonNode root = doSearchSingle(json);

        JsonNode results = root.get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Wittgenstein, Ludwig, 1889-1951.", result1.get("name").asText());
        assertEquals("/people/person", result1.get("type").get(0).get("id").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("24609378", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Klossowski, Pierre, 1905-2001.", result2.get("name").asText());
        assertEquals("/people/person", result2.get("type").get(0).get("id").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("27066848", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Anscombe, Gertrude Elizabeth Margaret, 1919-2001.", result3.get("name").asText());
        assertEquals("/people/person", result3.get("type").get(0).get("id").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("59078032", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchPersonalName() throws Exception {

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22wittgenstein%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/wittgenstein_personalnames.xml");

        String json = "{\"q0\":{\"query\": \"wittgenstein\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Wittgenstein, Ludwig, 1889-1951.", result1.get("name").asText());
        assertEquals("/people/person", result1.get("type").get(0).get("id").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("24609378", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Klossowski, Pierre, 1905-2001.", result2.get("name").asText());
        assertEquals("/people/person", result2.get("type").get(0).get("id").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("27066848", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Anscombe, Gertrude Elizabeth Margaret, 1919-2001.", result3.get("name").asText());
        assertEquals("/people/person", result3.get("type").get(0).get("id").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("59078032", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchNoParticularType() throws Exception {
        // This is when you get when you choose "Reconcile against no particular type" in OpenRefine

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22steinbeck%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/steinbeck_no_type.xml");

        String json = "{\"q0\":{\"query\": \"steinbeck\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Steinbeck, John 1946-1991", result1.get("name").asText());
        assertEquals("/people/person", result1.get("type").get(0).get("id").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("19893647", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Steinbeck, John, 1902-1968", result2.get("name").asText());
        assertEquals("/people/person", result2.get("type").get(0).get("id").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("96992551", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Steinbeck, John, 1902-1968. | Of mice and men.", result3.get("name").asText());
        assertEquals("/book/book", result3.get("type").get(0).get("id").asText());
        assertEquals("Work", result3.get("type").get(0).get("name").asText());
        assertEquals("180993990", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchWithSource() throws Exception {
        // Also chose "Reconcile against no particular type" for this one

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22nabokov%22%20and%20local.sources%20%3D%20%22nsk%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/nabokov_nsk.xml");

        String json = "{\"q0\":{\"query\": \"nabokov\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf/NSK").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Nabokov, Vladimir Vladimirovič", result1.get("name").asText());
        assertEquals("/people/person", result1.get("type").get(0).get("id").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("27069388", result1.get("id").asText());
        assertFalse(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Nabokov, Vladimir Vladimirovič | Lolita", result2.get("name").asText());
        assertEquals("/book/book", result2.get("type").get(0).get("id").asText());
        assertEquals("Work", result2.get("type").get(0).get("name").asText());
        assertEquals("176671347", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Nabokov, Vladimir Vladimirovič | Govori, sjećanje!", result3.get("name").asText());
        assertEquals("/book/book", result3.get("type").get(0).get("id").asText());
        assertEquals("Work", result3.get("type").get(0).get("name").asText());
        assertEquals("183561595", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchWithExactMatch() throws Exception {

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/shakespeare_exact.xml");

        String json = "{\"q0\":{\"query\": \"Shakespeare, William, 1564-1616.\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(3, results.size());

        JsonNode result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.get("name").asText());
        assertEquals("/people/person", result1.get("type").get(0).get("id").asText());
        assertEquals("Person", result1.get("type").get(0).get("name").asText());
        assertEquals("96994048", result1.get("id").asText());
        assertTrue(result1.get("match").asBoolean());

        JsonNode result2 = results.get(1);
        assertEquals("Hauptmann, Gerhart, 1862-1946.", result2.get("name").asText());
        assertEquals("/people/person", result2.get("type").get(0).get("id").asText());
        assertEquals("Person", result2.get("type").get(0).get("name").asText());
        assertEquals("71404832", result2.get("id").asText());
        assertFalse(result2.get("match").asBoolean());

        JsonNode result3 = results.get(2);
        assertEquals("Pasternak, Boris Leonidovich, 1890-1960.", result3.get("name").asText());
        assertEquals("/people/person", result3.get("type").get(0).get("id").asText());
        assertEquals("Person", result3.get("type").get(0).get("name").asText());
        assertEquals("68933968", result3.get("id").asText());
        assertFalse(result3.get("match").asBoolean());
    }

    @Test
    public void testSearchWithNoResults() throws Exception {

        String json = "{\"q0\":{\"query\": \"ncjecerence\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        String body = mvcResult.getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(body);

        JsonNode results = root.get("q0").get("result");

        assertEquals(0, results.size());
    }

    @Test
    public void testCache() throws Exception {

        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/shakespeare_exact.xml");

        String json = "{\"q0\":{\"query\": \"Shakespeare, William, 1564-1616.\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        JsonNode results = new ObjectMapper()
                .readTree(mvcResult.getResponse().getContentAsString())
                .get("q0").get("result");
        assertEquals(3, results.size());

        mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();
        assertEquals(3, results.size());

        JsonNode results2 = new ObjectMapper()
                .readTree(mvcResult.getResponse().getContentAsString())
                .get("q0").get("result");
        assertEquals(3, results2.size());

        Collection<Invocation> invocations = Mockito.mockingDetails(connectionFactory).getInvocations();

        assertEquals(1, invocations.size());
    }

    @Test
    public void testExpireCache() throws Exception {
        mockConnectionFactoryHelper.expect(connectionFactory,
                "https://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml",
                "/shakespeare_exact.xml", 2);

        String json = "{\"q0\":{\"query\": \"Shakespeare, William, 1564-1616.\",\"type\":\"/people/person\",\"type_strict\":\"should\"}}";

        MvcResult mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();

        JsonNode results = new ObjectMapper()
                .readTree(mvcResult.getResponse().getContentAsString())
                .get("q0").get("result");
        assertEquals(3, results.size());

        // sleep past the TTL
        Thread.sleep((TTL_SECONDS + 1) * 1000);

        mvcResult = mvc.perform(get("/reconcile/viaf").param("queries", json)).andReturn();
        assertEquals(3, results.size());

        JsonNode results2 = new ObjectMapper()
                .readTree(mvcResult.getResponse().getContentAsString())
                .get("q0").get("result");
        assertEquals(3, results2.size());

        Collection<Invocation> invocations = Mockito.mockingDetails(connectionFactory).getInvocations();

        assertEquals(2, invocations.size());
    }

    @AfterEach
    public void cleanup() throws Exception {
        cacheManager.getCache(Application.CACHE_DEFAULT).clear();
    }
}
