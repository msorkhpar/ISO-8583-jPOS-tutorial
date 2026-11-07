package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import com.example.practice.IssuerLedger.Result;

class IssuerLedgerTest {

    /** One transmission: its own STAN and field 7, the transaction's acquirer, terminal and RRN. */
    private static ISOMsg tx(String mti, String stan, String de7, String acquirer, String terminal,
                             String rrn, long amount) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "4111111111111111");
        m.set(4, String.format("%012d", amount));
        m.set(7, de7);
        m.set(11, stan);
        m.set(32, acquirer);
        m.set(37, rrn);
        m.set(41, terminal);
        return m;
    }

    // ---- main cases ----

    @Test
    void newTransactionsArePosted() throws Exception {
        IssuerLedger ledger = new IssuerLedger();

        assertEquals(Result.POSTED, ledger.handle(tx("0220", "004410", "0702164501", "77001", "29110001", "618301004410", 3399)));
        assertEquals(Result.POSTED, ledger.handle(tx("0200", "004411", "0702164502", "77001", "29110001", "618301004411", 1250)));
        assertEquals(4649, ledger.balance());
    }

    @Test
    void aReversalUndoesWhatWasPosted() throws Exception {
        IssuerLedger ledger = new IssuerLedger();
        ledger.handle(tx("0200", "004520", "0702170000", "77001", "29110005", "618301004520", 2200));
        ledger.handle(tx("0200", "004521", "0702170100", "77001", "29110005", "618301004521", 800));

        assertEquals(Result.REVERSED, ledger.handle(tx("0420", "004530", "0702170500", "77001", "29110005", "618301004520", 2200)));
        assertEquals(800, ledger.balance());
    }

    // ---- edge cases ----

    @Test
    void aRepeatWithItsOwnStanIsADuplicate() throws Exception {
        IssuerLedger ledger = new IssuerLedger();
        ledger.handle(tx("0220", "004610", "0702180000", "77001", "29110002", "618301004610", 5150));

        assertEquals(Result.DUPLICATE, ledger.handle(tx("0221", "004618", "0702180130", "77001", "29110002", "618301004610", 5150)));
        assertEquals(Result.DUPLICATE, ledger.handle(tx("0221", "004625", "0702180430", "77001", "29110002", "618301004610", 5150)));
        assertEquals(5150, ledger.balance());
    }

    @Test
    void twoAcquirersMayShareATerminalIdAndAnRrn() throws Exception {
        IssuerLedger ledger = new IssuerLedger();

        assertEquals(Result.POSTED, ledger.handle(tx("0220", "000017", "0702190000", "77001", "00000001", "000000000017", 1999)));
        assertEquals(Result.POSTED, ledger.handle(tx("0220", "000017", "0702190000", "88002", "00000001", "000000000017", 450)));
        assertEquals(2449, ledger.balance());
    }

    @Test
    void aRepeatWhoseFirstCopyNeverArrivedIsPosted() throws Exception {
        IssuerLedger ledger = new IssuerLedger();

        assertEquals(Result.POSTED, ledger.handle(tx("0221", "004713", "0702200100", "77001", "29110003", "618301004700", 6400)));
        assertEquals(Result.DUPLICATE, ledger.handle(tx("0220", "004700", "0702200000", "77001", "29110003", "618301004700", 6400)));
        assertEquals(6400, ledger.balance());
    }

    @Test
    void aReversalTakesEffectOnce() throws Exception {
        IssuerLedger ledger = new IssuerLedger();
        ledger.handle(tx("0200", "004800", "0702210000", "77001", "29110004", "618301004800", 3000));
        ledger.handle(tx("0200", "004801", "0702210100", "77001", "29110004", "618301004801", 700));

        assertEquals(Result.REVERSED, ledger.handle(tx("0420", "004810", "0702210500", "77001", "29110004", "618301004800", 3000)));
        assertEquals(Result.ALREADY_REVERSED, ledger.handle(tx("0421", "004822", "0702210800", "77001", "29110004", "618301004800", 3000)));
        assertEquals(Result.ALREADY_REVERSED, ledger.handle(tx("0420", "004810", "0702210500", "77001", "29110004", "618301004800", 3000)));
        assertEquals(700, ledger.balance());
    }

    @Test
    void anOriginalThatArrivesAfterItsReversalIsNeverPosted() throws Exception {
        IssuerLedger ledger = new IssuerLedger();

        assertEquals(Result.NOTHING_TO_REVERSE,
                ledger.handle(tx("0420", "004905", "0702220200", "77001", "29110006", "618301004900", 1500)));
        assertEquals(Result.LATE_ORIGINAL,
                ledger.handle(tx("0200", "004900", "0702220000", "77001", "29110006", "618301004900", 1500)));
        assertEquals(Result.LATE_ORIGINAL,
                ledger.handle(tx("0200", "004900", "0702220000", "77001", "29110006", "618301004900", 1500)));
        assertEquals(0, ledger.balance());
    }
}
