package com.example.practice;

import java.util.Map;
import java.util.TreeMap;

/**
 * Reads field 48 of a network message, kept by the packager as plain text: an optional
 * transaction category code, then subelements of a two-digit tag, a two-digit length and the data.
 */
public final class Field48Reader {

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
        // TODO: the field's category code, when it has one.
        throw new IllegalStateException("TODO: categoryCode");
    }

    /** The data of the subelement with this two-digit tag, or null when the field has none. */
    static String value(String field48, String tag) {
        // TODO: the data the sender put under this tag.
        throw new IllegalStateException("TODO: value");
    }
}
