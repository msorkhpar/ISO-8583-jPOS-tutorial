package com.example.practice;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

/** One business day's 1987 reconciliation totals, and the 0500 / 0510 exchange that compares them. */
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
                t.creditReversals++;
                t.creditReversalsAmount += amount;
            } else {
                t.debitReversals++;
                t.debitReversalsAmount += amount;
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

    /** The totals fields an 0510 compares: 74-77, 81, 86-89 and 97. */
    public static final int[] TOTALS_FIELDS = { 74, 75, 76, 77, 81, 86, 87, 88, 89, 97 };

    /** The acquirer's 0500 for one business day. */
    public static ISOMsg request(Totals t, String de15, String stan, String de7, String acquirer)
            throws ISOException {
        ISOMsg m = new ISOMsg("0500");
        m.set(7, de7);
        m.set(11, stan);
        m.set(15, de15);
        m.set(32, acquirer);
        m.set(50, "840");
        m.set(74, ISOUtil.zeropad(t.credits(), 10));
        m.set(75, ISOUtil.zeropad(t.creditReversals(), 10));
        m.set(76, ISOUtil.zeropad(t.debits(), 10));
        m.set(77, ISOUtil.zeropad(t.debitReversals(), 10));
        m.set(81, ISOUtil.zeropad(t.authorizations(), 10));
        m.set(86, ISOUtil.zeropad(t.creditsAmount(), 16));
        m.set(87, ISOUtil.zeropad(t.creditReversalsAmount(), 16));
        m.set(88, ISOUtil.zeropad(t.debitsAmount(), 16));
        m.set(89, ISOUtil.zeropad(t.debitReversalsAmount(), 16));
        m.set(97, de97(t.net()));
        return m;
    }

    /** DE 97, the net settlement amount: C when the sender of the totals is owed it, D when it owes it. */
    static String de97(long net) throws ISOException {
        // TODO: The packager takes a sign character and 16 digits. (one line)
        throw new IllegalStateException("TODO: de97");
    }

    /** The receiver's 0510 to an 0500, given the totals the receiver counted for the same day. */
    public static ISOMsg answer(ISOMsg request, Totals ours) throws ISOException {
        // TODO: The answer says whether the receiver's count of the day agrees with the sender's:
        //       DE 39 00 (the request was processed) and DE 66, 1 in balance or 2 out of balance.
        //       (about 10 lines)
        throw new IllegalStateException("TODO: answer");
    }
}
