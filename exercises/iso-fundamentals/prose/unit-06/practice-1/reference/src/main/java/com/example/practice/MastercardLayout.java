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
        return """
              <isofield id="0" length="4" name="MTI" class="org.jpos.iso.IFE_NUMERIC"/>
              <isofield id="1" length="16" name="BITMAP" class="org.jpos.iso.IFB_BITMAP"/>
              <isofield id="2" length="19" name="PAN" class="org.jpos.iso.IFE_LLNUM"/>
              <isofield id="3" length="6" name="PROCESSING CODE" class="org.jpos.iso.IFE_NUMERIC"/>
              <isofield id="4" length="12" name="AMOUNT" class="org.jpos.iso.IFE_NUMERIC"/>
              <isofield id="11" length="6" name="STAN" class="org.jpos.iso.IFE_NUMERIC"/>
              <isofield id="41" length="8" name="TERMINAL ID" class="org.jpos.iso.IFE_CHAR"/>
              <isofield id="42" length="15" name="CARD ACCEPTOR ID" class="org.jpos.iso.IFE_CHAR"/>
              <isofield id="49" length="3" name="CURRENCY CODE" class="org.jpos.iso.IFE_NUMERIC"/>
            """;
    }
}
