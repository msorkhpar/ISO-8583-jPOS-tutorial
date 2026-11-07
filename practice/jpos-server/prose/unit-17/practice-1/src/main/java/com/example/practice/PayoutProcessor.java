package com.example.practice;

import org.jpos.util.DirPoll;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The acquirer's payout run, delivered as a file: a header H|acquirer|run, one P|merchant|amount line
 * per merchant (a 15-character merchant id, the amount in minor units), and a trailer
 * T|count|total. Pays every merchant through the {@link Ledger} and answers with the totals.
 */
public class PayoutProcessor implements DirPoll.Processor {
    public record Payout(String merchant, long amount) {}

    private final Ledger ledger;

    public PayoutProcessor(Ledger ledger) {
        this.ledger = ledger;
    }

    @Override
    public byte[] process(String name, byte[] content) throws DirPoll.DirPollException {
        List<String> lines = new ArrayList<>();
        for (String l : new String(content, StandardCharsets.US_ASCII).split("\r?\n"))
            if (!l.isBlank())
                lines.add(l.trim());
        if (!finished(lines))
            throw notYet(name);
        String[] h = lines.get(0).split("\\|");
        if (h.length != 3 || !h[0].equals("H"))
            throw refused(name, "no header");
        String run = h[1] + "/" + h[2];
        List<Payout> payouts = payoutsOf(name, lines.subList(1, lines.size() - 1));
        checkTrailer(name, lines.get(lines.size() - 1), payouts);
        ledger.pay(run, payouts);
        long total = payouts.stream().mapToLong(Payout::amount).sum();
        return String.format("PAID %s merchants=%d total=%d%n", run, payouts.size(), total)
                .getBytes(StandardCharsets.US_ASCII);
    }

    /** Is this the whole run, header to trailer? */
    boolean finished(List<String> lines) {
        // TODO: A sender may still be writing, or may not have started. (one line)
        throw new IllegalStateException("TODO: finished");
    }

    /** The exception for a file that is not complete yet. */
    DirPoll.DirPollException notYet(String name) {
        // TODO: DirPoll should come back to this file later. (about three lines)
        throw new IllegalStateException("TODO: notYet");
    }

    /** The exception for a file that will never be right. */
    DirPoll.DirPollException refused(String name, String why) {
        // TODO: A person has to look at this file; DirPoll should not see it again. (one line)
        throw new IllegalStateException("TODO: refused");
    }

    /** The payouts of the P| records, in order. */
    List<Payout> payoutsOf(String name, List<String> records) throws DirPoll.DirPollException {
        // TODO: A record is P|merchant|amount: a 15-character merchant id and 1 to 12 digits.
        //       Anything else makes the whole file wrong. (about eight lines)
        throw new IllegalStateException("TODO: payoutsOf");
    }

    /** The trailer must agree with the payouts. */
    void checkTrailer(String name, String trailer, List<Payout> payouts) throws DirPoll.DirPollException {
        // TODO: T|count|total, where total is the sum of the amounts. (about five lines)
        throw new IllegalStateException("TODO: checkTrailer");
    }

    /** Given: the payments table. Each call to pay is one database transaction. */
    public static final class Ledger {
        private final List<String> payments = new ArrayList<>();

        public synchronized void pay(String run, List<Payout> payouts) {
            for (Payout p : payouts)
                payments.add(run + " " + p.merchant() + " " + p.amount());
        }

        public synchronized List<String> payments() {
            return new ArrayList<>(payments);
        }
    }
}
