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
        return lines.size() >= 2 && lines.get(lines.size() - 1).startsWith("T|");
    }

    /** The exception for a file that is not complete yet. */
    DirPoll.DirPollException notYet(String name) {
        DirPoll.DirPollException e = new DirPoll.DirPollException(name + ": not complete yet");
        e.setRetry(true);
        return e;
    }

    /** The exception for a file that will never be right. */
    DirPoll.DirPollException refused(String name, String why) {
        return new DirPoll.DirPollException(name + ": " + why);
    }

    /** The payouts of the P| records, in order. */
    List<Payout> payoutsOf(String name, List<String> records) throws DirPoll.DirPollException {
        List<Payout> payouts = new ArrayList<>();
        for (String r : records) {
            String[] f = r.split("\\|");
            if (f.length != 3 || !f[0].equals("P") || f[1].length() != 15 || !f[2].matches("\\d{1,12}"))
                throw notYet(name);
            payouts.add(new Payout(f[1], Long.parseLong(f[2])));
        }
        return payouts;
    }

    /** The trailer must agree with the payouts. */
    void checkTrailer(String name, String trailer, List<Payout> payouts) throws DirPoll.DirPollException {
        String[] t = trailer.split("\\|");
        long total = payouts.stream().mapToLong(Payout::amount).sum();
        if (t.length != 3 || !t[1].equals(Integer.toString(payouts.size())) || !t[2].equals(Long.toString(total)))
            throw refused(name, "trailer does not match the payouts");
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
