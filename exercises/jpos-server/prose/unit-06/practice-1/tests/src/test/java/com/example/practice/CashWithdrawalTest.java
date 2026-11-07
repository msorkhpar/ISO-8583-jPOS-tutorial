package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashWithdrawalTest {

    /** A withdrawal as the server's channel hands it over: packed by the ATM's side, unpacked by ours. */
    private static ISOMsg withdrawal(String amount, String currency) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "011000");
        if (amount != null) m.set(4, amount);
        m.set(7, "0714152233");
        m.set(11, "004417");
        m.set(41, "29110007");
        if (currency != null) m.set(49, currency);
        GenericPackager p = new GenericPackager("jar:packager/iso87ascii.xml");
        m.setPackager(p);
        byte[] wire = m.pack();
        ISOMsg in = new ISOMsg();
        in.setPackager(p);
        in.unpack(wire);
        return in;
    }

    private record Outcome(int vote, ISOMsg response) {
        boolean prepared() { return (vote & TransactionConstants.PREPARED) != 0; }
        String rc() { return response.getString(39); }
    }

    private static Outcome run(ISOMsg request) throws Exception {
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), request);
        int vote = new CashWithdrawal().prepare(7L, ctx);
        ISOMsg response = ctx.get(ContextConstants.RESPONSE.toString());
        assertNotNull(response, "every request gets a response in the Context");
        assertEquals("0210", response.getMTI());
        assertEquals("004417", response.getString(11));
        return new Outcome(vote, response);
    }

    @Test
    void aWithdrawalInNotesIsApproved() throws Exception {
        Outcome o = run(withdrawal("000000016000", "978"));             // 160.00 euro
        assertEquals("00", o.rc());
        assertTrue(o.prepared());
    }

    @Test
    void anAmountTheNotesCannotMakeIsRefused() throws Exception {
        Outcome o = run(withdrawal("000000016550", "978"));             // 165.50 euro
        assertEquals("13", o.rc());
        assertTrue(!o.prepared());
    }

    @Test
    void aWithdrawalOverTheMaximumIsRefused() throws Exception {
        assertEquals("61", run(withdrawal("000000041000", "978")).rc()); // 410.00 euro
    }

    @Test
    void aCurrencyWithoutACassetteIsRefused() throws Exception {
        assertEquals("57", run(withdrawal("000000002000", "826")).rc()); // pounds: none loaded
    }

    @Test
    void aWithdrawalWithoutACurrencyIsAFormatError() throws Exception {
        Outcome o = run(withdrawal("000000016000", null));
        assertEquals("30", o.rc());
        assertTrue(!o.prepared());
    }

    @Test
    void aSignedAmountIsAFormatError() throws Exception {
        assertEquals("30", run(withdrawal("-00000016000", "978")).rc());
    }

    @Test
    void wonHaveNoMinorUnit() throws Exception {
        assertEquals("00", run(withdrawal("000000070000", "410")).rc()); // 70,000 won
        assertEquals("61", run(withdrawal("000001200000", "410")).rc()); // 1,200,000 won
    }

    @Test
    void dinarsHaveThreeDecimals() throws Exception {
        assertEquals("00", run(withdrawal("000000025000", "414")).rc()); // 25.000 dinars
        assertEquals("13", run(withdrawal("000000025500", "414")).rc()); // 25.500 dinars
    }

    @Test
    void anUnknownCurrencyCodeIsAFormatError() throws Exception {
        assertEquals("30", run(withdrawal("000000016000", "555")).rc());
    }
}
