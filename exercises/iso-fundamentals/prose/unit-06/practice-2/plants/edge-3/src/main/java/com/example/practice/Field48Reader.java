package com.example.practice;

import java.util.Map;
import java.util.TreeMap;

/**
 * Reads field 48 of a network message, kept by the packager as plain text: an optional
 * transaction category code, then subelements of a two-digit tag, a two-digit length and the data.
 */
public final class Field48Reader {

    /** The subelements this application uses. */
    private static final java.util.Set<String> KNOWN = java.util.Set.of("10", "42", "61", "77");

    private Field48Reader() {
    }

    /** The decisions the issuer's rules need from field 48, by tag. */
    public static Map<String, String> wanted(String field48, String... tags) {
        Map<String, String> found = new TreeMap<>();
        for (String tag : tags) {
            String v = value(field48, tag);
            if (v != null)
                found.put(tag, v);
        }
        return found;
    }

    /** The category code, or null when the field starts with a subelement. */
    static String categoryCode(String field48) {
        char first = field48.charAt(0);
        return Character.isDigit(first) ? null : String.valueOf(first);
    }

    /** The data of the subelement with this two-digit tag, or null when the field has none. */
    static String value(String field48, String tag) {
        int at = categoryCode(field48) == null ? 0 : 1;
        while (at + 4 <= field48.length()) {
            String t = field48.substring(at, at + 2);
            int len = Integer.parseInt(field48.substring(at + 2, at + 4));
            if (t.equals(tag))
                return field48.substring(at + 4, at + 4 + len);
            if (!KNOWN.contains(t))
                throw new IllegalArgumentException("unknown subelement " + t);
            at += 4 + len;
        }
        return null;
    }
}
