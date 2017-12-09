package com.codefork.refine.orcid;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class OrcidTest {

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
