package com.example.practice;

import com.example.practice.TicketSearch.Ticket;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketSearchTest {
    private static final ZoneId LONDON = ZoneId.of("Europe/London");     // UTC in December
    private static final ZoneId ATHENS = ZoneId.of("Europe/Athens");     // two hours ahead of UTC
    private static final DateTimeFormatter DE7 = DateTimeFormatter.ofPattern("MMddHHmmss");

    private TimeZone machine;

    @BeforeEach
    void machineOnUtc() {
        machine = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restoreMachine() {
        TimeZone.setDefault(machine);
    }

    /** A purchase sent at {@code utc} by a terminal in {@code zone}, and its answer. */
    private static List<ISOMsg> exchange(String terminal, String stan, String utc, ZoneId zone, String rc)
            throws Exception {
        Instant at = Instant.parse(utc);
        ZonedDateTime local = at.atZone(zone);
        ISOMsg request = new ISOMsg("0200");
        request.set(3, "000000");
        request.set(4, "000000003675");
        request.set(7, at.atZone(ZoneId.of("UTC")).format(DE7));
        request.set(11, stan);
        request.set(12, local.format(DateTimeFormatter.ofPattern("HHmmss")));
        request.set(13, local.format(DateTimeFormatter.ofPattern("MMdd")));
        request.set(41, terminal);
        ISOMsg answer = (ISOMsg) request.clone();
        answer.setResponseMTI();
        answer.set(39, rc);
        return List.of(request, answer);
    }

    @SafeVarargs
    private static List<ISOMsg> log(List<ISOMsg>... exchanges) {
        List<ISOMsg> all = new ArrayList<>();
        for (List<ISOMsg> e : exchanges)
            all.addAll(e);
        return all;
    }

    private static void assertFound(List<ISOMsg> expected, List<ISOMsg> found) {
        assertEquals(expected.size(), found.size(), "messages found");
        for (int i = 0; i < expected.size(); i++)
            assertSame(expected.get(i), found.get(i), "message " + i + " of the result");
    }

    @Test
    void findsTheRequestAndItsAnswer() throws Exception {
        List<ISOMsg> meant = exchange("29110041", "004521", "2026-12-12T09:30:15Z", LONDON, "05");
        List<ISOMsg> log = log(
                exchange("29110041", "004519", "2026-12-12T09:28:40Z", LONDON, "00"),
                meant,
                exchange("29110041", "004522", "2026-12-12T09:31:02Z", LONDON, "00"),
                exchange("29110043", "000017", "2026-12-12T09:30:15Z", LONDON, "00"));
        assertFound(meant, TicketSearch.find(log, new Ticket("29110041", "004521", Instant.parse("2026-12-12T09:30:15Z"))));
    }

    @Test
    void findsNothingForAMessageThatNeverLeft() throws Exception {
        List<ISOMsg> log = log(
                exchange("29110041", "004519", "2026-12-12T09:28:40Z", LONDON, "00"),
                exchange("29110043", "000017", "2026-12-12T09:30:15Z", LONDON, "00"));
        assertTrue(TicketSearch.find(log, new Ticket("29110041", "004520", Instant.parse("2026-12-12T09:29:51Z"))).isEmpty(),
                "a message the log never saw is not in it");
    }

    @Test
    void anotherTerminalWithTheSameStanAtTheSameSecond() throws Exception {
        List<ISOMsg> meant = exchange("29110041", "004521", "2026-12-12T09:30:15Z", LONDON, "05");
        List<ISOMsg> log = log(
                exchange("29110042", "004521", "2026-12-12T09:30:15Z", LONDON, "00"),
                meant);
        assertFound(meant, TicketSearch.find(log, new Ticket("29110041", "004521", Instant.parse("2026-12-12T09:30:15Z"))));
    }

    @Test
    void theSameTerminalUsesTheStanAgainTheNextDay() throws Exception {
        List<ISOMsg> meant = exchange("29110041", "000088", "2026-12-12T08:05:44Z", LONDON, "05");
        List<ISOMsg> log = log(
                exchange("29110041", "000088", "2026-12-11T07:58:03Z", LONDON, "00"),
                meant);
        assertFound(meant, TicketSearch.find(log, new Ticket("29110041", "000088", Instant.parse("2026-12-12T08:05:44Z"))));
    }

    @Test
    void aTerminalAwayFromUtcIsFoundAtTheTicketsTime() throws Exception {
        List<ISOMsg> meant = exchange("29110051", "002230", "2026-12-12T09:30:15Z", ATHENS, "05");
        List<ISOMsg> log = log(
                exchange("29110051", "002229", "2026-12-12T09:12:40Z", ATHENS, "00"),
                meant);
        assertFound(meant, TicketSearch.find(log, new Ticket("29110051", "002230", Instant.parse("2026-12-12T09:30:15Z"))));
    }

    @Test
    void theMachinesTimeZoneDoesNotMatter() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        List<ISOMsg> meant = exchange("29110041", "004521", "2026-12-12T09:30:15Z", LONDON, "05");
        List<ISOMsg> log = log(
                exchange("29110041", "004519", "2026-12-12T09:28:40Z", LONDON, "00"),
                meant);
        assertFound(meant, TicketSearch.find(log, new Ticket("29110041", "004521", Instant.parse("2026-12-12T09:30:15Z"))));
    }

    @Test
    void aStanTypedWithoutItsLeadingZeros() throws Exception {
        List<ISOMsg> meant = exchange("29110041", "000930", "2026-12-12T10:02:09Z", LONDON, "05");
        List<ISOMsg> log = log(
                exchange("29110041", "000929", "2026-12-12T10:01:30Z", LONDON, "00"),
                meant);
        assertFound(meant, TicketSearch.find(log, new Ticket("29110041", "930", Instant.parse("2026-12-12T10:02:09Z"))));
    }
}
