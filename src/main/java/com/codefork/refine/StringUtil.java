package com.codefork.refine;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;

public class StringUtil {

    /**
     * Shamelessly copied from this website with only a tiny modification:
     * https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     */
    public static int levenshteinDistance(CharSequence lhs, CharSequence rhs) {
        // if strings are the same, dist is 0
        if (lhs.equals(rhs)) {
            return 0;
        }

        int len0 = lhs.length() + 1;
        int len1 = rhs.length() + 1;

        // the array of distances
        int[] cost = new int[len0];
        int[] newcost = new int[len0];

        // initial cost of skipping prefix in String s0
        for (int i = 0; i < len0; i++) cost[i] = i;

        // dynamically computing the array of distances

        // transformation cost for each letter in s1
        for (int j = 1; j < len1; j++) {
            // initial cost of skipping prefix in String s1
            newcost[0] = j;

            // transformation cost for each letter in s0
            for (int i = 1; i < len0; i++) {
                // matching current letters in both strings
                int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int cost_replace = cost[i - 1] + match;
                int cost_insert = cost[i] + 1;
                int cost_delete = newcost[i - 1] + 1;

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }

            // swap cost/newcost arrays
            int[] swap = cost;
            cost = newcost;
            newcost = swap;
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[len0 - 1];
    }

    /**
     * Returns string similarity as a ratio. This is calculated as a ratio of
     * levenshstein distance to max possible distance (which is the length of
     * the longer string). The more dissimilar the strings are, the lower
     * the value returned. Identical strings return 1.0.
     *
     * @param lhs
     * @param rhs
     * @return
     */
    public static double levenshteinDistanceRatio(CharSequence lhs, CharSequence rhs) {
        int maxDist = lhs.length();
        if (rhs.length() > maxDist) {
            maxDist = rhs.length();
        }
        if (maxDist == 0) {
            return 1;
        }
        return (maxDist - Double.valueOf(levenshteinDistance(lhs, rhs))) / Double.valueOf(maxDist);
    }

    /**
     * consumes an InputStream into a String
     *
     * @param is
     * @param bufferSize
     * @return
     */
    public static String inputStreamToString(final InputStream is, final int bufferSize) {
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        try {
            Reader in = new InputStreamReader(is, "UTF-8");
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
        } catch (UnsupportedEncodingException ex) {
        } catch (IOException ex) {
        }
        return out.toString();
    }

    public static String join(List<String> strings, String sep) {
        StringBuilder buf = new StringBuilder();
        for (String s : strings) {
            if (buf.length() > 0) {
                buf.append(sep);
            }
            buf.append(s);
        }
        return buf.toString();
    }

    public static String getStackTrace(Throwable aThrowable) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}