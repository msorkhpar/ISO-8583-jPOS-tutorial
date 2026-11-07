package com.example.practice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes one data element as an ASCII receiver expects it, for a test tool that builds raw
 * ISO 8583 messages without a packager. Formats are written the way specifications write them:
 * {@code n6}, {@code an12}, {@code n..19}, {@code ans...999}. Types are {@code n}, {@code a},
 * {@code an} and {@code ans}.
 */
public final class FieldWriter {

    /** A parsed format: its type, its prefix width (0 for a fixed field) and its length or maximum. */
    record Format(String type, int prefixDigits, int length) { }

    private static final Pattern FORMAT = Pattern.compile("(ans|an|a|n)(\\.{2,3})?(\\d+)");

    private FieldWriter() {
    }

    /** The field exactly as it goes on the wire; refuses with IllegalArgumentException what the receiver would reject. */
    public static String write(String format, String value) {
        Format f = parse(format);
        if (!allowed(f.type(), value))
            throw new IllegalArgumentException("a character outside " + f.type() + ": " + value);
        return f.prefixDigits() == 0 ? fixed(f, value) : variable(f, value);
    }

    static Format parse(String format) {
        Matcher m = FORMAT.matcher(format);
        if (!m.matches())
            throw new IllegalArgumentException("not a field format: " + format);
        int prefix = m.group(2) == null ? 0 : m.group(2).length();
        return new Format(m.group(1), prefix, Integer.parseInt(m.group(3)));
    }

    /** Whether every character of the value belongs to the type. */
    static boolean allowed(String type, String value) {
        String chars = switch (type) {
            case "n" -> "[0-9]*";
            case "a" -> "[A-Za-z]*";
            case "an" -> "[A-Za-z0-9]*";
            default -> "[\\x20-\\x7E]*";
        };
        return value.matches(chars);
    }

    /** A fixed field: always f.length() characters. */
    static String fixed(Format f, String value) {
        if (value.length() > f.length())
            value = value.substring(0, f.length());                  // cut to fit
        String fill = (f.type().equals("n") ? "0" : " ").repeat(f.length() - value.length());
        return f.type().equals("n") ? fill + value : value + fill;
    }

    /** A variable field: its length, then the value. */
    static String variable(Format f, String value) {
        if (value.length() > f.length())
            throw new IllegalArgumentException("longer than " + f.length() + ": " + value);
        String len = String.valueOf(value.length());
        return "0".repeat(f.prefixDigits() - len.length()) + len + value;
    }
}
