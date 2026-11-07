package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

class DayTotalsTest {

    private static final String DAY = "0312";

    private static ISOMsg request(DayTotals.Totals t) throws Exception {
        return DayTotals.request(t, DAY, "000417", "0312230510", "445566");
    }

    // ---- main cases ----

    @Test
    void the0500CarriesTheNetAndPacks() throws Exception {
        ISOMsg m = request(new DayTotals.Totals(2, 0, 3, 1, 4, 1675, 0, 10439, 1299));

        assertEquals("C0000000000007465", m.getString(97));
        assertEquals("0000000000010439", m.getString(88));
        m.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        m.pack();
    }

    @Test
    void equalTotalsAreAnsweredInBalance() throws Exception {
        ISOMsg r = DayTotals.answer(request(new DayTotals.Totals(2, 0, 3, 1, 4, 1675, 0, 10439, 1299)),
                new DayTotals.Totals(2, 0, 3, 1, 4, 1675, 0, 10439, 1299));

        assertEquals("0510", r.getMTI());
        assertEquals("00", r.getString(39));
        assertEquals("1", r.getString(66));
        assertEquals("000417", r.getString(11));
        assertEquals(DAY, r.getString(15));
    }

    @Test
    void aMissingPurchaseIsOutOfBalance() throws Exception {
        ISOMsg r = DayTotals.answer(request(new DayTotals.Totals(0, 0, 3, 0, 0, 0, 0, 10439, 0)),
                new DayTotals.Totals(0, 0, 2, 0, 0, 0, 0, 7439, 0));

        assertEquals("2", r.getString(66));
    }

    // ---- edge cases ----

    @Test
    void aDayWithMoreRefundsThanSalesSendsADebitNet() throws Exception {
        ISOMsg m = request(new DayTotals.Totals(3, 0, 1, 0, 0, 12000, 0, 2550, 0));

        assertEquals("D0000000000009450", m.getString(97));
    }

    @Test
    void theSameNetWithOtherCountsIsOutOfBalance() throws Exception {
        ISOMsg sent = request(new DayTotals.Totals(0, 0, 2, 1, 0, 0, 0, 9140, 3000));
        DayTotals.Totals subtracted = new DayTotals.Totals(0, 0, 1, 0, 0, 0, 0, 6140, 0);

        assertEquals("2", DayTotals.answer(sent, subtracted).getString(66));
    }

    @Test
    void theReceived0500StaysAsItWasReceived() throws Exception {
        ISOMsg sent = request(new DayTotals.Totals(0, 0, 3, 0, 0, 0, 0, 10439, 0));

        DayTotals.answer(sent, new DayTotals.Totals(0, 0, 3, 0, 0, 0, 0, 10439, 0));

        assertEquals("0500", sent.getMTI());
        assertFalse(sent.hasField(39));
        assertFalse(sent.hasField(66));
    }
}
