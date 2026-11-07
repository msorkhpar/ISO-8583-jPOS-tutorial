package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jpos.iso.IFA_NUMERIC;
import org.jpos.iso.ISOException;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

class LayoutCheckTest {

    /** A purchase from the host's specification, in its ASCII layout. */
    private static final byte[] SAMPLE = ("0200" + "7020000000800000" + "16" + "5555555555554444"
            + "000000" + "000000008800" + "000452" + "ATM00417").getBytes(StandardCharsets.US_ASCII);

    private static final Map<Integer, String> DOCUMENTED = Map.of(
            2, "5555555555554444", 3, "000000", 4, "000000008800", 11, "000452", 41, "ATM00417");

    /** The host's layout. */
    private static GenericPackager hostLayout() throws ISOException {
        return new GenericPackager("jar:packager/iso87ascii.xml");
    }

    @Test
    void theHostsOwnLayoutHasNothingToFix() throws Exception {
        assertEquals(0, LayoutCheck.firstWrongField(hostLayout(), SAMPLE.clone(), DOCUMENTED));
    }

    @Test
    void anAmountRowOneDigitTooLong() throws Exception {
        GenericPackager layout = hostLayout();
        layout.setFieldPackager(4, new IFA_NUMERIC(13, "AMOUNT, TRANSACTION"));
        assertEquals(4, LayoutCheck.firstWrongField(layout, SAMPLE.clone(), DOCUMENTED));
    }

    @Test
    void aProcessingCodeRowOneDigitTooShort() throws Exception {
        GenericPackager layout = hostLayout();
        layout.setFieldPackager(3, new IFA_NUMERIC(5, "PROCESSING CODE"));
        assertEquals(3, LayoutCheck.firstWrongField(layout, SAMPLE.clone(), DOCUMENTED));
    }

    @Test
    void aLayoutWithNoRowForTheTerminal() throws Exception {
        GenericPackager layout = hostLayout();
        layout.setFieldPackager(41, null);
        assertEquals(41, LayoutCheck.firstWrongField(layout, SAMPLE.clone(), DOCUMENTED));
    }
}
