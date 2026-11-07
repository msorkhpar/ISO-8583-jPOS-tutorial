package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class DayTotalsTest {

    private static final String DAY = "0312";

    private static ISOMsg entry(String mti, String pcode, long amount, String rc, String de15) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(3, pcode);
        m.set(4, String.format("%012d", amount));
        if (rc != null)
            m.set(39, rc);
        m.set(15, de15);
        return m;
    }

    private static ISOMsg purchase(long amount) throws Exception {
        return entry("0200", "000000", amount, "00", DAY);
    }

    private static ISOMsg refund(long amount) throws Exception {
        return entry("0200", "200000", amount, "00", DAY);
    }

    private static ISOMsg reversal(String mti, String pcode, long amount, String original) throws Exception {
        ISOMsg m = entry(mti, pcode, amount, null, DAY);
        m.set(90, original);
        return m;
    }

    private static final String SALE_A = "020000081703120941050000004455600000000000";
    private static final String SALE_B = "020000081903121002170000004455600000000000";
    private static final String REFUND_C = "020000082403121130400000004455600000000000";

    /** credits, credit reversals, debits, debit reversals, authorizations, then the four amounts. */
    private static long[] all(DayTotals.Totals t) {
        return new long[] { t.credits(), t.creditReversals(), t.debits(), t.debitReversals(), t.authorizations(),
                t.creditsAmount(), t.creditReversalsAmount(), t.debitsAmount(), t.debitReversalsAmount() };
    }

    private static long[] count(ISOMsg... journal) {
        return all(DayTotals.count(List.of(journal), DAY));
    }

    // ---- main cases ----

    @Test
    void purchasesAreDebitsAndRefundsAreCredits() throws Exception {
        assertArrayEquals(new long[] { 2, 0, 3, 0, 0, 1675, 0, 10439, 0 },
                count(purchase(6140), refund(875), purchase(1299), purchase(3000),
                        entry("0200", "200030", 800, "00", DAY)));
    }

    // ---- edge cases ----

    @Test
    void aDeclinedRequestCountsNowhere() throws Exception {
        assertArrayEquals(new long[] { 0, 0, 1, 0, 0, 0, 0, 6140, 0 },
                count(purchase(6140), entry("0200", "000000", 25000, "51", DAY),
                        entry("0200", "200000", 990, "05", DAY)));
    }

    @Test
    void anAuthorizationIsCountedButMovesNoMoney() throws Exception {
        assertArrayEquals(new long[] { 0, 0, 1, 0, 1, 0, 0, 1299, 0 },
                count(entry("0100", "000000", 45000, "00", DAY), entry("0100", "000000", 7000, "05", DAY),
                        purchase(1299)));
    }

    @Test
    void aReversalAndItsRepeatAreOneReversal() throws Exception {
        assertArrayEquals(new long[] { 0, 0, 2, 2, 0, 0, 0, 9140, 9140 },
                count(purchase(6140), purchase(3000), reversal("0420", "000000", 6140, SALE_A),
                        reversal("0421", "000000", 6140, SALE_A), reversal("0421", "000000", 6140, SALE_A),
                        reversal("0420", "000000", 3000, SALE_B)));
    }

    @Test
    void aReversedPurchaseStaysCountedAsADebit() throws Exception {
        assertArrayEquals(new long[] { 0, 0, 2, 1, 0, 0, 0, 7439, 1299 },
                count(purchase(6140), purchase(1299), reversal("0420", "000000", 1299, SALE_B)));
    }

    @Test
    void aRefundsReversalIsACreditReversal() throws Exception {
        assertArrayEquals(new long[] { 1, 1, 1, 0, 0, 875, 875, 3000, 0 },
                count(purchase(3000), refund(875), reversal("0420", "200000", 875, REFUND_C)));
    }

    @Test
    void entriesOfAnotherDayWaitForTheirOwn() throws Exception {
        assertArrayEquals(new long[] { 0, 0, 1, 0, 0, 0, 0, 6140, 0 },
                count(purchase(6140), entry("0200", "000000", 1299, "00", "0313"),
                        entry("0200", "200000", 875, "00", "0311")));
    }

    @Test
    void anAcknowledgedReversalCountsWithoutAField39() throws Exception {
        assertArrayEquals(new long[] { 0, 0, 1, 1, 0, 0, 0, 3000, 3000 },
                count(purchase(3000), reversal("0420", "000000", 3000, SALE_B)));
    }
}
