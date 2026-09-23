package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class RequestFactoryTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-01T10:43:21Z"), ZoneId.of("Asia/Tokyo"));

    @Test
    void authorizationAndFinancialShareTheCommonFields() throws Exception {
        RequestFactory factory = new RequestFactory("TERM0001", CLOCK, 41);
        ISOMsg auth = factory.authorization("4111111111111111", 10000);
        ISOMsg fin = factory.financial("5500000000000004", 2550);

        assertEquals("0100", auth.getMTI());
        assertEquals("0200", fin.getMTI());
        assertEquals("4111111111111111", auth.getString(2));
        assertEquals("5500000000000004", fin.getString(2));
        assertEquals("000000010000", auth.getString(4));
        assertEquals("000000002550", fin.getString(4));
        for (ISOMsg m : new ISOMsg[] {auth, fin}) {
            assertEquals("000000", m.getString(3));
            assertEquals("840", m.getString(49));
            assertEquals("0701104321", m.getString(7));
            assertEquals("TERM0001", m.getString(41));
        }
        assertEquals("000042", auth.getString(11));
        assertEquals("000043", fin.getString(11));
    }

    @Test
    void aReversalPointsBackAtTheOriginal() throws Exception {
        RequestFactory factory = new RequestFactory("TERM0001", CLOCK, 122);
        ISOMsg original = factory.financial("4111111111111111", 10000);
        ISOMsg reversal = factory.reversal(original);

        assertEquals("0400", reversal.getMTI());
        assertEquals("4111111111111111", reversal.getString(2));
        assertEquals("000000", reversal.getString(3));
        assertEquals("000000010000", reversal.getString(4));
        assertEquals("840", reversal.getString(49));
        assertEquals("TERM0001", reversal.getString(41));
        assertEquals("0200" + "000123" + "0701104321" + "0".repeat(22), reversal.getString(90));
    }

    @Test
    void theTraceNumberWrapsPastSixNines() throws Exception {
        RequestFactory factory = new RequestFactory("TERM0001", CLOCK, 999998);
        assertEquals("999999", factory.create("0800").getString(11));
        assertEquals("000001", factory.create("0800").getString(11));
        assertEquals("000002", factory.create("0800").getString(11));
    }

    @Test
    void aReversalGetsItsOwnTraceNumber() throws Exception {
        RequestFactory factory = new RequestFactory("TERM0001", CLOCK, 122);
        ISOMsg original = factory.financial("4111111111111111", 10000);
        ISOMsg reversal = factory.reversal(original);

        assertEquals("000123", original.getString(11));
        assertEquals("000124", reversal.getString(11));
        assertNotEquals(original.getString(11), reversal.getString(11));
    }
}
