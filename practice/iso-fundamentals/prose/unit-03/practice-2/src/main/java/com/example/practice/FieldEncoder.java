package com.example.practice;

/** Writes a data element's value the way its format says. */
public final class FieldEncoder {

    private FieldEncoder() {
    }

    /** The field as it goes on the wire, for a format such as n6, an12 or ans..40. */
    public static String encode(String format, String value) {
        throw new UnsupportedOperationException("write encode");
    }
}
