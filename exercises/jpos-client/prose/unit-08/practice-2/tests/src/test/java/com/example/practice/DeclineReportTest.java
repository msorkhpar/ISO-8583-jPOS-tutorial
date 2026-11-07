package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.Logger;
import org.jpos.util.ProtectedLogListener;
import org.jpos.util.SimpleLogListener;
import org.junit.jupiter.api.Test;

class DeclineReportTest {
    private static final String PAN = "5555555555554444";
    private static final String TRACK2 = PAN + "=30091010000000000000";
    private static final String TRACK1 = "B" + PAN + "^TEST/CARD^30091010000000000000";
    private static final String PIN_HEX = "9F3A5C7E10B2D486";

    private final ByteArrayOutputStream written = new ByteArrayOutputStream();

    private static GenericPackager packager() throws Exception {
        return new GenericPackager("jar:packager/iso87ascii.xml");
    }

    /** The node's logger: the filter first, then a writer. */
    private Logger nodeLogger() throws Exception {
        ProtectedLogListener protect = new ProtectedLogListener();
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("protect", "2 35 45");
        cfg.put("wipe", "52 55");
        protect.setConfiguration(cfg);
        Logger logger = new Logger();
        logger.addListener(protect);
        logger.addListener(new SimpleLogListener(new PrintStream(written, true, StandardCharsets.UTF_8)));
        return logger;
    }

    private static ISOMsg request() throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, PAN);
        m.set(3, "000000");
        m.set(4, "000000012345");
        m.set(11, "004219");
        m.set(35, TRACK2);
        m.set(41, "29110001");
        m.set(45, TRACK1);
        m.set(52, ISOUtil.hex2byte(PIN_HEX));
        return m;
    }

    private static ISOMsg rejection(ISOMsg request) throws Exception {
        ISOMsg r = (ISOMsg) request.clone();
        r.setResponseMTI();
        r.set(39, "30");
        return r;
    }

    private String log() {
        return written.toString(StandardCharsets.UTF_8);
    }

    private static String chars(byte[] image) {
        return new String(image, StandardCharsets.ISO_8859_1);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1))
            n++;
        return n;
    }

    @Test
    void theLogShowsBothMessagesFieldByField() throws Exception {
        DeclineReport report = new DeclineReport(nodeLogger(), packager());
        ISOMsg req = request();
        report.report(req, rejection(req));
        String log = log();
        assertTrue(count(log, "<field id=\"11\" value=\"004219\"/>") >= 2, "request and response, field by field:\n" + log);
        assertTrue(log.contains("<field id=\"39\" value=\"30\"/>"), "the response code is in the entry:\n" + log);
    }

    @Test
    void theDeskGetsTheRejectedMessage() throws Exception {
        DeclineReport report = new DeclineReport(nodeLogger(), packager());
        ISOMsg req = request();
        String dump = report.report(req, rejection(req));
        assertFalse(dump.isBlank(), "a dump comes back");
        ISOMsg seen = new ISOMsg();
        seen.setPackager(packager());
        seen.unpack(report.shareableImage(request()));
        assertEquals("0200", seen.getMTI());
        assertEquals("004219", seen.getString(11));
        assertEquals("000000012345", seen.getString(4));
    }

    @Test
    void noCardDataReachesTheLog() throws Exception {
        DeclineReport report = new DeclineReport(nodeLogger(), packager());
        ISOMsg req = request();
        report.report(req, rejection(req));
        String log = log();
        assertFalse(log.contains(PAN), "the card number reached the writer:\n" + log);
        assertFalse(log.contains(PIN_HEX), "the PIN block reached the writer:\n" + log);
    }

    @Test
    void theDumpHoldsNoCardNumber() throws Exception {
        DeclineReport report = new DeclineReport(nodeLogger(), packager());
        String image = chars(report.shareableImage(request()));
        assertFalse(image.contains(PAN), "the card number is in the bytes:\n" + image);
    }

    @Test
    void theDumpHoldsNoPinBlock() throws Exception {
        DeclineReport report = new DeclineReport(nodeLogger(), packager());
        String image = chars(report.shareableImage(request()));
        assertFalse(image.contains(PIN_HEX), "the PIN block is in the bytes:\n" + image);
    }

    @Test
    void theRequestComesOutAsItWentIn() throws Exception {
        DeclineReport report = new DeclineReport(nodeLogger(), packager());
        ISOMsg req = request();
        report.report(req, rejection(req));
        assertEquals(PAN, req.getString(2), "the client still needs the real card number");
        assertEquals(TRACK2, req.getString(35));
        assertEquals(TRACK1, req.getString(45));
        assertArrayEquals(ISOUtil.hex2byte(PIN_HEX), req.getBytes(52));
    }

    @Test
    void theDumpIsLaidOutLikeTheMessageTheHostReceived() throws Exception {
        DeclineReport report = new DeclineReport(nodeLogger(), packager());
        ISOMsg sent = request();
        sent.setPackager(packager());
        byte[] wire = sent.pack();
        byte[] image = report.shareableImage(request());
        assertEquals(wire.length, image.length, "every field keeps its length");
        assertArrayEquals(Arrays.copyOfRange(wire, 0, 20), Arrays.copyOfRange(image, 0, 20),
                "the MTI and the bitmap are the host's");
    }
}
