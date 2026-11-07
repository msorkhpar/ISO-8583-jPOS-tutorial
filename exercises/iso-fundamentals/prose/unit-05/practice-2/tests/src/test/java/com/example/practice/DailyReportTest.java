package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.practice.DailyReport.Kind;
import java.util.List;
import java.util.Map;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class DailyReportTest {

    private static ISOMsg msg(String mti, String processingCode, String amount) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        if (processingCode != null)
            m.set(3, processingCode);
        if (amount != null)
            m.set(4, amount);
        m.set(41, "29110001");
        return m;
    }

    @Test
    void namesFinancialMessagesByTheirType() throws Exception {
        assertEquals(Kind.PURCHASE, DailyReport.kind(msg("0200", "000000", "000000001250")));
        assertEquals(Kind.CASH, DailyReport.kind(msg("0200", "010000", "000000006000")));
        assertEquals(Kind.REFUND, DailyReport.kind(msg("0200", "200000", "000000000799")));
    }

    @Test
    void addsUpADay() throws Exception {
        Map<Kind, Long> totals = DailyReport.totals(List.of(
                msg("0200", "000000", "000000001250"),
                msg("0200", "000000", "000000000350"),
                msg("0200", "200000", "000000000799"),
                msg("0800", null, null)));
        assertEquals(1600L, totals.get(Kind.PURCHASE));
        assertEquals(799L, totals.get(Kind.REFUND));
        assertEquals(null, totals.get(Kind.REVERSAL));
    }

    @Test
    void networkMessagesAreNoTransaction() throws Exception {
        ISOMsg echo = msg("0800", null, null);
        echo.set(70, "301");
        assertEquals(Kind.OTHER, DailyReport.kind(echo));
    }

    @Test
    void accountDigitsDoNotChangeTheType() throws Exception {
        assertEquals(Kind.PURCHASE, DailyReport.kind(msg("0200", "002000", "000000004410")));
        assertEquals(Kind.CASH, DailyReport.kind(msg("0200", "011000", "000000002000")));
        assertEquals(Kind.REFUND, DailyReport.kind(msg("0200", "200020", "000000000399")));
    }

    @Test
    void aReversalIsNamedByItsClass() throws Exception {
        assertEquals(Kind.REVERSAL, DailyReport.kind(msg("0420", "010000", "000000006000")));
        assertEquals(Kind.REVERSAL, DailyReport.kind(msg("0420", "200000", "000000000799")));
    }

    @Test
    void anAuthorizationNamesItsTransactionToo() throws Exception {
        assertEquals(Kind.PURCHASE, DailyReport.kind(msg("0100", "000000", "000000007700")));
        assertEquals(Kind.CASH, DailyReport.kind(msg("0120", "010000", "000000010000")));
    }

    @Test
    void everyReversalClassMessageIsAReversal() throws Exception {
        assertEquals(Kind.REVERSAL, DailyReport.kind(msg("0400", "000000", "000000001250")));
        assertEquals(Kind.REVERSAL, DailyReport.kind(msg("0421", "000000", "000000001250")));
    }
}
