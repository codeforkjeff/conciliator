package com.codefork.refine.viaf;

import com.codefork.refine.StringUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StringUtilTest {

    @Test
    public void testLevenshteinDistance() {
        String s = "this is a test sentence";
        assertEquals(0, StringUtil.levenshteinDistance(s, "this is a test sentence"));
        assertEquals(1, StringUtil.levenshteinDistance(s, "this is A test sentence"));
        assertEquals(s.length(), StringUtil.levenshteinDistance(s, ""));
    }

    @Test
    public void testLevenshteinDistanceRatio() {
        assertEquals(1.0, StringUtil.levenshteinDistanceRatio("same", "same"), 0.0);
        assertEquals(0.0, StringUtil.levenshteinDistanceRatio("completely different", "x"), 0.0);
        assertEquals(0.5, StringUtil.levenshteinDistanceRatio("1324", "1234"), 0.0);
        assertTrue(
                StringUtil.levenshteinDistanceRatio("Charles Dickens", "Dickens, Charles, 1812-1870") >
                StringUtil.levenshteinDistanceRatio("Charles Dickens", "Chesterton, G.K. (Gilbert Keith), 1874-1936"));
    }

}
