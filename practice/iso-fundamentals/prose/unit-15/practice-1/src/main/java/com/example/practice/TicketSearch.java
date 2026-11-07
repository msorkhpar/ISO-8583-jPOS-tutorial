package com.example.practice;

import org.jpos.iso.ISOMsg;

import java.time.Instant;
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
        // TODO: Analysts type STANs the way people say them. Return the value a message's
        //       field 11 holds for the STAN on the ticket.
        throw new IllegalStateException("TODO: stan");
    }

    /** The ticket's moment as a message carries it. */
    static String sent(Ticket ticket) {
        // TODO: Return the value a message holds for the moment on the ticket, in the field
        //       that records when the message was sent.
        throw new IllegalStateException("TODO: sent");
    }

    /** Whether {@code m} is one of the messages the ticket is about. */
    static boolean names(Ticket ticket, ISOMsg m) {
        // TODO: True only for the message the analyst means and its answer, whatever else the
        //       day's log holds from other terminals and other days.
        throw new IllegalStateException("TODO: names");
    }
}
