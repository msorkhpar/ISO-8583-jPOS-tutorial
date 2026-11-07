package com.example.practice;

import org.jpos.iso.ISOMsg;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Finds the messages of a day's log that an acquirer's ticket is about. */
public final class TicketSearch {

    /**
     * What the ticket says: the terminal ID, the STAN as the analyst typed it, and the moment the
     * acquirer's records say the message was sent.
     */
    public record Ticket(String terminal, String stan, Instant sent) { }

    private TicketSearch() { }

    /** Every logged message the ticket names, request and answer alike, in log order. */
    public static List<ISOMsg> find(List<ISOMsg> log, Ticket ticket) {
        List<ISOMsg> found = new ArrayList<>();
        for (ISOMsg m : log)
            if (names(ticket, m))
                found.add(m);
        return found;
    }

    /** The ticket's STAN as a message carries it. */
    static String stan(Ticket ticket) {
        return "000000".substring(ticket.stan().length()) + ticket.stan();
    }

    /** The ticket's moment as a message carries it. */
    static String sent(Ticket ticket) {
        return DateTimeFormatter.ofPattern("MMddHHmmss").withZone(ZoneOffset.UTC).format(ticket.sent());
    }

    /** Whether {@code m} is one of the messages the ticket is about. */
    static boolean names(Ticket ticket, ISOMsg m) {
        return ticket.terminal().equals(m.getString(41))
                && stan(ticket).equals(m.getString(11));
    }
}
