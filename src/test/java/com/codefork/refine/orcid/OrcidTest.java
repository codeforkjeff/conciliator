package com.codefork.refine.orcid;

import com.codefork.refine.Config;
import com.codefork.refine.resources.ServiceMetaDataResponse;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class OrcidTest {

    @Test
    public void testOrcidServiceMetaData() {
        Config config = new Config();
        Orcid orcid = new Orcid();
        orcid.setConfig(config);
        orcid.init();

        ServiceMetaDataResponse response = orcid.serviceMetaData();
        assertEquals(response.getName(), "ORCID");
    }

    @Test
    public void testParseName() throws Exception {
        assertArrayEquals(Orcid.parseName("joe schmoe"),
                new String[] { "joe", "schmoe" });
        assertArrayEquals(Orcid.parseName("schmoe, joe"),
                new String[] { "joe", "schmoe" });
        assertArrayEquals(Orcid.parseName("dr. joe schmoe"),
                null);
    }
}
