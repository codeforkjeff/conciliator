package com.codefork.refine.viaf;

import com.codefork.refine.Config;
import org.junit.Test;
import com.codefork.refine.NameType;
import com.codefork.refine.SearchQuery;
import com.codefork.refine.resources.Result;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class VIAFTest {

    VIAFService viafService;
    Config config;

    @Test
    public void testSearchPersonalName() throws Exception {
        viafService = mock(VIAFService.class);
        InputStream is = getClass().getResourceAsStream("/wittgenstein.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        VIAF viaf = new VIAF(viafService, config);

        SearchQuery query = new SearchQuery("wittgenstein", 3, NameType.Person, "should");
        List<Result> results = viaf.search(query);

        assertEquals(3, results.size());

        Result result1 = results.get(0);
        assertEquals("Wittgenstein, Ludwig, 1889-1951", result1.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result1.getType().get(0));
        assertEquals("24609378", result1.getId());
        assertFalse(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Anscombe, G. E. M. (Gertrude Elizabeth Margaret)", result2.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result2.getType().get(0));
        assertEquals("59078032", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Klossowski, Pierre, 1905-2001", result3.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result3.getType().get(0));
        assertEquals("27066848", result3.getId());
        assertFalse(result3.isMatch());
    }

    @Test
    public void testSearchNoParticularType() throws Exception {
        // This is when you get when you choose "Reconcile against no particular type" in OpenRefine

        // http://www.viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22John%20Steinbeck%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        viafService = mock(VIAFService.class);
        InputStream is = getClass().getResourceAsStream("/steinbeck_no_type.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        VIAF viaf = new VIAF(viafService, config);

        SearchQuery query = new SearchQuery("steinbeck", 3, null, "should");
        List<Result> results = viaf.search(query);

        assertEquals(3, results.size());
        Result result1 = results.get(0);
        assertEquals("Steinbeck, John, 1902-1968", result1.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result1.getType().get(0));
        assertEquals("96992551", result1.getId());
        assertFalse(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Steinbeck, John 1946-1991", result2.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result2.getType().get(0));
        assertEquals("19893647", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Steinbeck, John, 1902-1968. | Of mice and men.", result3.getName());
        assertEquals(NameType.Book.asVIAFNameType(), result3.getType().get(0));
        assertEquals("180993990", result3.getId());
        assertFalse(result3.isMatch());
    }

    @Test
    public void testSearchWithSource() throws Exception {
        // Also chose "Reconcile against no particular type" for this one

        // https://viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22Vladimir%20Nabokov%22%20and%20local.sources%20%3D%20%22nsk%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        viafService = mock(VIAFService.class);
        InputStream is = getClass().getResourceAsStream("/nabokov_nsk.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        VIAF viaf = new VIAF(viafService, config);

        SearchQuery query = new SearchQuery("nabokov", 3, null, "should");
        query.setSource("NSK"); // NSK=Croatia
        List<Result> results = viaf.search(query);

        assertEquals(3, results.size());
        Result result1 = results.get(0);
        assertEquals("Nabokov, Vladimir Vladimirovič", result1.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result1.getType().get(0));
        assertEquals("27069388", result1.getId());
        assertFalse(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Nabokov, Vladimir Vladimirovič | Lolita", result2.getName());
        assertEquals(NameType.Book.asVIAFNameType(), result2.getType().get(0));
        assertEquals("176671347", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Nabokov, Vladimir Vladimirovič | Govori, sjećanje!", result3.getName());
        assertEquals(NameType.Book.asVIAFNameType(), result3.getType().get(0));
        assertEquals("183561595", result3.getId());
        assertFalse(result3.isMatch());
    }

    @Test
    public void testSearchWithExactMatch() throws Exception {
        // http://www.viaf.org/viaf/search?query=local.personalNames%20all%20%22Shakespeare,%20William,%201564-1616.%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        viafService = mock(VIAFService.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        VIAF viaf = new VIAF(viafService, config);

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, NameType.Person, "should");
        List<Result> results = viaf.search(query);

        assertEquals(3, results.size());
        Result result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result1.getType().get(0));
        assertEquals("96994048", result1.getId());
        assertTrue(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Zamenhof, L. L. (Ludwik Lazar), 1859-1917", result2.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result2.getType().get(0));
        assertEquals("73885295", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Tieck, Ludwig, 1773-1853", result3.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result3.getType().get(0));
        assertEquals("34463780", result3.getId());
        assertFalse(result3.isMatch());
    }

    @Test
    public void testSearchWithNoResults() throws Exception {

        // https://viaf.org/viaf/search?query=local.mainHeadingEl%20all%20%22ncjecerence%22%20and%20local.sources%20%3D%20%22nsk%22&sortKeys=holdingscount&maximumRecords=3&httpAccept=application/xml
        viafService = mock(VIAFService.class);
        InputStream is = getClass().getResourceAsStream("/nonsense.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        VIAF viaf = new VIAF(viafService, config);

        SearchQuery query = new SearchQuery("ncjecerence", 3, null, "should");
        List<Result> results = viaf.search(query);

        assertEquals(0, results.size());
    }

    @Test
    public void testSearchProxyModeLC() throws Exception {
        viafService = mock(VIAFService.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        VIAF viaf = new VIAF(viafService, config);

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, NameType.Person, "should", true);
        query.setSource("LC");

        List<Result> results = viaf.search(query);

        assertEquals(3, results.size());
        Result result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result1.getType().get(0));
        assertEquals("n78095332", result1.getId());
        assertTrue(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Zamenhof, L. L. (Ludwik Lazar), 1859-1917", result2.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result2.getType().get(0));
        assertEquals("no90015706", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Tieck, Ludwig, 1773-1853", result3.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result3.getType().get(0));
        assertEquals("n78096841", result3.getId());
        assertFalse(result3.isMatch());
    }

    /**
     * VIAF gives a URL for the BNF source record ID. this test
     * checks that we use the ID parsed out of the "sid" XML element instead.
     */
    @Test
    public void testSearchProxyModeBNF() throws Exception {
        viafService = mock(VIAFService.class);
        InputStream is = getClass().getResourceAsStream("/shakespeare.xml");
        when(viafService.doSearch(anyString(), anyInt())).thenReturn(is);

        config = mock(Config.class);
        when(config.getProperties()).thenReturn(new Properties());

        VIAF viaf = new VIAF(viafService, config);

        SearchQuery query = new SearchQuery("Shakespeare, William, 1564-1616.", 3, NameType.Person, "should", true);
        query.setSource("BNF");

        List<Result> results = viaf.search(query);

        assertEquals(3, results.size());
        Result result1 = results.get(0);
        assertEquals("Shakespeare, William, 1564-1616.", result1.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result1.getType().get(0));
        assertEquals("11924607", result1.getId());
        assertTrue(result1.isMatch());

        Result result2 = results.get(1);
        assertEquals("Zamenhof, Lejzer Ludwik, 1859-1917", result2.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result2.getType().get(0));
        assertEquals("12115775", result2.getId());
        assertFalse(result2.isMatch());

        Result result3 = results.get(2);
        assertEquals("Tieck, Ludwig, 1773-1853", result3.getName());
        assertEquals(NameType.Person.asVIAFNameType(), result3.getType().get(0));
        assertEquals("11926644", result3.getId());
        assertFalse(result3.isMatch());
    }

}