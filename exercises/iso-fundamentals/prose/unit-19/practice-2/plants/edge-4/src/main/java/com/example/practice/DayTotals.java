package com.example.practice;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jpos.iso.ISOMsg;

/** One business day's 1987 reconciliation totals, counted from the acquirer's journal. */
public final class DayTotals {

    /** The totals of one day; amounts in minor units. */
    public record Totals(long credits, long creditReversals, long debits, long debitReversals,
                         long authorizations,
                         long creditsAmount, long creditReversalsAmount,
                         long debitsAmount, long debitReversalsAmount) {

        /** What the sender of the totals is owed (positive) or owes (negative). */
        public long net() {
            return (debitsAmount - debitReversalsAmount) - (creditsAmount - creditReversalsAmount);
        }
    }

    /** The running counts while a journal is read. */
    static final class Tally {
        long credits, creditReversals, debits, debitReversals, authorizations;
        long creditsAmount, creditReversalsAmount, debitsAmount, debitReversalsAmount;
        final Set<String> reversed = new HashSet<>();      // DE 90 of every reversal counted so far

        Totals totals() {
            return new Totals(credits, creditReversals, debits, debitReversals, authorizations,
                    creditsAmount, creditReversalsAmount, debitsAmount, debitReversalsAmount);
        }
    }

    private DayTotals() {
    }

    /**
     * The totals of the business day {@code de15}. Each journal entry is a message as the exchange
     * ended: the request's fields, plus DE 15 and, for requests, the DE 39 of the answer. Reversals
     * (0420, 0421) are in the journal once acknowledged, and carry no DE 39.
     */
    public static Totals count(List<ISOMsg> journal, String de15) {
        Tally t = new Tally();
        for (ISOMsg m : journal)
            if (onTheDay(m, de15) && settled(m))
                add(t, m);
        return t.totals();
    }

    /** Whether the entry belongs to the day being counted. */
    static boolean onTheDay(ISOMsg m, String de15) {
        return de15.equals(m.getString(15));
    }

    /** Whether the entry did what it asked for, so that it has something to count. */
    static boolean settled(ISOMsg m) {
        return isReversal(m) || "00".equals(m.getString(39));
    }

    static boolean isReversal(ISOMsg m) {
        return m.getString(0).startsWith("04");
    }

    /** True for a refund: processing code 20, money back to the card. */
    static boolean isRefund(ISOMsg m) {
        return m.getString(3).startsWith("20");
    }

    /** Puts one settled entry of the day into the totals. */
    static void add(Tally t, ISOMsg m) {
        long amount = Long.parseLong(m.getString(4));
        if (isReversal(m)) {
            if (!t.reversed.add(m.getString(90)))
                return;
            if (isRefund(m)) {
                t.credits--;                                // the refund is undone
                t.creditsAmount -= amount;
            } else {
                t.debits--;                                 // the purchase is undone
                t.debitsAmount -= amount;
            }
        } else if (m.getString(0).startsWith("01")) {
            t.authorizations++;
        } else if (isRefund(m)) {
            t.credits++;
            t.creditsAmount += amount;
        } else {
            t.debits++;
            t.debitsAmount += amount;
        }
    }
}
