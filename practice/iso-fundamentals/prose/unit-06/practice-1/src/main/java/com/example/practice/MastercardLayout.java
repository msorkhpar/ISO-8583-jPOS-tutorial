package com.example.practice;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.jpos.iso.ISOException;
import org.jpos.iso.packager.GenericPackager;

/** The packager for the new link, written from its specification's field table and samples. */
public final class MastercardLayout {

    private static final String HEAD = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE isopackager SYSTEM "genericpackager.dtd">
        <isopackager>
        """;
    private static final String TAIL = "</isopackager>\n";

    private MastercardLayout() {
    }

    /** The packager the link uses, built from the field definitions below. */
    public static GenericPackager packager() throws ISOException {
        String xml = HEAD + fields() + TAIL;
        return new GenericPackager(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** One isofield element per field of the table in the statement: 0, 1, 2, 3, 4, 11, 41, 42 and 49. */
    static String fields() {
        // TODO: one <isofield .../> line per field of the specification's table, so that every
        //       message this packager writes has exactly the bytes of the specification's samples.
        throw new IllegalStateException("TODO: fields");
    }
}
