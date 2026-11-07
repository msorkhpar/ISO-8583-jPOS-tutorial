package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalTest {

    /** A request as the issuer received it. Values are built at run time, so == fails. */
    private static ISOMsg request(String mti, String stan, String when, String acquirer, String forwarder)
            throws Exception {
        ISOMsg m = new ISOMsg(new String(mti));
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000004250");
        m.set(7, new String(when));
        m.set(11, new String(stan));
        if (acquirer != null) m.set(32, new String(acquirer));
        if (forwarder != null) m.set(33, new String(forwarder));
        m.set(41, "29110021");
        return m;
    }

    /** A reversal as an acquirer sends it: its own STAN and time, field 90 naming the original. */
    private static ISOMsg reversal(String mti, String stan, String field90) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "5555555555554444");
        m.set(4, "000000004250");
        m.set(7, "0315231000");
        m.set(11, stan);
        m.set(41, "29110021");
        m.set(90, new String(field90));
        return m;
    }

    @Test
    void aReversalCancelsTheRequestItNames() throws Exception {
        Journal journal = new Journal();
        ISOMsg a = request("0200", "004416", "0315225901", "12345678901", "98765432109");
        ISOMsg b = request("0200", "004417", "0315230002", "12345678901", "98765432109");
        ISOMsg c = request("0100", "004418", "0315230510", "12345678901", "98765432109");
        journal.record(a);
        journal.record(b);
        journal.record(c);

        ISOMsg ack = journal.onReversal(reversal("0420", "000901",
                "0200" + "004417" + "0315230002" + "12345678901" + "98765432109"));

        assertEquals("0430", ack.getMTI());
        assertEquals("000901", ack.getString(11));
        assertTrue(journal.isReversed(b));
        assertFalse(journal.isReversed(a));
        assertFalse(journal.isReversed(c));
        assertEquals("0200", b.getMTI(), "the recorded request is unchanged");
    }

    @Test
    void aReversalOfSomethingNeverSeenIsAcknowledged() throws Exception {
        Journal journal = new Journal();
        ISOMsg a = request("0200", "004416", "0315225901", "12345678901", "98765432109");
        journal.record(a);

        ISOMsg ack = journal.onReversal(reversal("0420", "000902",
                "0200" + "004499" + "0315231500" + "12345678901" + "98765432109"));

        assertEquals("0430", ack.getMTI());
        assertEquals("00", ack.getString(39));
        assertFalse(journal.isReversed(a));
    }

    @Test
    void theSameStanOnAnotherDayIsAnotherRequest() throws Exception {
        Journal journal = new Journal();
        ISOMsg yesterday = request("0200", "000318", "0314101500", "12345678901", "98765432109");
        ISOMsg today = request("0200", "000318", "0315101500", "12345678901", "98765432109");
        journal.record(yesterday);
        journal.record(today);

        journal.onReversal(reversal("0420", "000903",
                "0200" + "000318" + "0315101500" + "12345678901" + "98765432109"));

        assertTrue(journal.isReversed(today));
        assertFalse(journal.isReversed(yesterday), "yesterday's purchase stands");
    }

    @Test
    void shortInstitutionIdsStillName() throws Exception {
        Journal journal = new Journal();
        ISOMsg purchase = request("0200", "007001", "0315120000", "45678901", null);
        journal.record(purchase);

        journal.onReversal(reversal("0420", "000904",
                "0200" + "007001" + "0315120000" + "00045678901" + "00000000000"));

        assertTrue(journal.isReversed(purchase));
    }

    @Test
    void aRepeatFindsTheOriginalToo() throws Exception {
        Journal journal = new Journal();
        ISOMsg purchase = request("0200", "006120", "0315140000", "12345678901", "98765432109");
        journal.record(purchase);

        ISOMsg ack = journal.onReversal(reversal("0421", "000905",
                "0200" + "006120" + "0315140000" + "12345678901" + "98765432109"));

        assertEquals("0430", ack.getMTI());
        assertTrue(journal.isReversed(purchase), "the first 0420 never arrived; the repeat is the one we see");
    }
}
