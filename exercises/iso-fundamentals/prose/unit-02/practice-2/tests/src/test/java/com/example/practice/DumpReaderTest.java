package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

class DumpReaderTest {

    /** What a hex-ASCII link carries for m: packed with the packager jPOS ships for it. */
    private static String capture(ISOMsg m) throws Exception {
        m.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        return new String(m.pack());
    }

    private static ISOMsg authorization() throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(2, "5555555555554444");
        m.set(3, "300000");
        m.set(4, "000000012999");
        m.set(7, "0314221507");
        m.set(11, "582031");
        m.set(12, "171507");
        m.set(13, "0314");
        m.set(22, "051");
        m.set(32, "40000210");
        m.set(33, "40000211");
        m.set(41, "29110001");
        m.set(49, "978");
        m.set(64, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        return m;
    }

    private static ISOMsg reversal() throws Exception {
        ISOMsg m = new ISOMsg("0420");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000004075");
        m.set(7, "0314221612");
        m.set(11, "582044");
        m.set(32, "40000210");
        m.set(41, "29110001");
        m.set(90, "020058204303142215580004000021000000000000");
        m.set(128, new byte[] { 8, 7, 6, 5, 4, 3, 2, 1 });
        return m;
    }

    @Test
    void readsTheFieldsAndWhereTheyStart() throws Exception {
        DumpReader auth = new DumpReader(capture(authorization()));
        assertEquals("0100", auth.mti());
        assertEquals(List.of(2, 3, 4, 7, 11, 12, 13, 22, 32, 33, 41, 49, 64), auth.fields());
        assertEquals(20, auth.dataStart());

        ISOMsg answer = new ISOMsg("0810");
        answer.set(8, "00000001");
        answer.set(9, "61000000");
        answer.set(39, "00");
        DumpReader echo = new DumpReader(capture(answer));
        assertEquals(List.of(8, 9, 39), echo.fields());
        assertEquals(20, echo.dataStart());
    }

    @Test
    void bitOneIsNotAField() throws Exception {
        DumpReader rev = new DumpReader(capture(reversal()));
        assertEquals("0420", rev.mti());
        assertEquals(List.of(2, 3, 4, 7, 11, 32, 41, 90, 128), rev.fields());
    }

    @Test
    void theFieldsStartAfterTheSecondBitmap() throws Exception {
        String dump = capture(reversal());
        assertEquals(36, new DumpReader(dump).dataStart());
        assertEquals("16", dump.substring(36, 38));       // the card number's length prefix
    }

    @Test
    void aThirdBitmapIsRefusedNotReadAsAField() {
        String dump = "0200" + "F000000000000000" + "8000000000000040"
            + "1655555555555544440000000000000040750000000000000000";
        DumpReader reader = new DumpReader(dump);
        assertThrows(IllegalArgumentException.class, reader::fields);
    }
}
