package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransmissionWindowTest {
    private TimeZone original;

    @BeforeEach
    void serverInUtc() {
        original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restoreZone() {
        TimeZone.setDefault(original);
    }

    private static Clock at(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private static ISOMsg request(String field7) throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(3, "000000");
        m.set(4, "000000004200");
        if (field7 != null) m.set(7, field7);
        m.set(11, "000918");
        m.set(41, "29110003");
        return m;
    }

    private record Outcome(int vote, Context ctx) {
        boolean passed() { return (vote & TransactionConstants.PREPARED) != 0; }
        Instant sentAt() { return ctx.get(TransmissionWindow.SENT_AT); }
        ISOMsg response() { return ctx.get(ContextConstants.RESPONSE.toString()); }
    }

    private static Outcome run(Clock clock, ISOMsg request) {
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), request);
        return new Outcome(new TransmissionWindow(clock).prepare(3L, ctx), ctx);
    }

    private static void assertRefused(Outcome o) throws Exception {
        assertEquals(0, o.vote() & TransactionConstants.PREPARED, "refused");
        assertNotNull(o.response(), "a refused request still gets its answer");
        assertEquals("0110", o.response().getMTI());
        assertEquals("30", o.response().getString(39));
        assertNull(o.sentAt());
    }

    @Test
    void aRecentRequestPassesWithItsSendingTime() throws Exception {
        Outcome o = run(at("2026-08-19T13:42:10Z"), request("0819134030"));
        assertEquals(true, o.passed());
        assertEquals(Instant.parse("2026-08-19T13:40:30Z"), o.sentAt());
        assertNull(o.response(), "a request that passes is answered further down the chain");
    }

    @Test
    void anOldRequestIsRefused() throws Exception {
        assertRefused(run(at("2026-08-19T13:42:10Z"), request("0819131000")));
    }

    @Test
    void aRequestFromJustBeforeNewYearPasses() throws Exception {
        Outcome o = run(at("2028-01-01T00:01:15Z"), request("1231235920"));
        assertEquals(true, o.passed());
        assertEquals(Instant.parse("2027-12-31T23:59:20Z"), o.sentAt());
    }

    @Test
    void aServerOutsideUtcReadsTheSameMoment() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        Outcome o = run(at("2026-08-19T13:42:10Z"), request("0819134030"));
        assertEquals(true, o.passed());
        assertEquals(Instant.parse("2026-08-19T13:40:30Z"), o.sentAt());
    }

    @Test
    void aRequestFromTheFutureIsRefused() throws Exception {
        assertRefused(run(at("2026-08-19T13:42:10Z"), request("0819135500")));
    }

    @Test
    void anUnreadableField7IsAnsweredWithAFormatError() throws Exception {
        assertRefused(run(at("2026-08-19T13:42:10Z"), request("08191340 0")));
    }

    @Test
    void aMissingField7IsAnsweredWithAFormatError() throws Exception {
        assertRefused(run(at("2026-08-19T13:42:10Z"), request(null)));
    }
}
