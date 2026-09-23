package com.example.practice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Writes a data element's value the way its format says. */
public final class FieldEncoder {

    private static final Pattern FORMAT = Pattern.compile("(ans|an|a|n|z)(\\.\\.)?(\\d+)");

    private FieldEncoder() {
    }

    /** The field as it goes on the wire, for a format such as n6, an12 or ans..40. */
    public static String encode(String format, String value) {
        Matcher parts = FORMAT.matcher(format);
        if (!parts.matches()) {
            throw new IllegalArgumentException("not a field format: " + format);
        }
        String type = parts.group(1);
        boolean variable = parts.group(2) != null;
        String length = parts.group(3);
        int most = Integer.parseInt(length);
        if (!value.matches(allowed(type) + "*")) {
            throw new IllegalArgumentException("a character outside type " + type);
        }
        if (value.length() > most) {
            throw new IllegalArgumentException("longer than " + most + " characters");
        }
        if (variable) {
            return pad(String.valueOf(value.length()), length.length(), '0', true) + value;
        }
        return pad(value, most, ' ', false);
    }

    private static String allowed(String type) {
        switch (type) {
            case "n": return "[0-9]";
            case "a": return "[A-Za-z]";
            case "an": return "[A-Za-z0-9]";
            case "z": return "[0-9=]";
            default: return "[\\x20-\\x7E]";
        }
    }

    private static String pad(String value, int width, char filler, boolean left) {
        StringBuilder fill = new StringBuilder();
        while (fill.length() + value.length() < width) {
            fill.append(filler);
        }
        return left ? fill + value : value + fill;
    }
}
