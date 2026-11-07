package com.example.practice;

import com.example.practice.Account.ClearingRecord;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountTest {

    private static ISOMsg message(String mti, long amount, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, String.format("%012d", amount));
        m.set(11, stan);
        m.set(41, "29110001");
        return m;
    }

    private static ISOMsg standInAdvice(long amount, String stan, String networkCode) throws Exception {
        ISOMsg m = message("0120", amount, stan);
        m.set(38, networkCode);
        return m;
    }

    @Test
    void heldThenClearedForTheSameAmount() throws Exception {
        Account account = new Account(100_000);
        ISOMsg answer = account.authorize(message("0100", 40_000, "000611"));
        assertEquals("00", answer.getString(39));

        account.clear(new ClearingRecord(answer.getString(38), 40_000));

        assertEquals(60_000, account.ledger(), "ledger after clearing");
        assertEquals(60_000, account.available(), "available after clearing");
    }

    @Test
    void aStandInApprovalIsAcknowledgedAndCounted() throws Exception {
        Account account = new Account(100_000);

        ISOMsg ack = account.standIn(standInAdvice(25_000, "000612", "N70311"));

        assertEquals("0130", ack.getMTI());
        assertEquals("00", ack.getString(39));
        assertEquals("000612", ack.getString(11));
        assertEquals(100_000, account.ledger(), "an approval alone moves no money");
        assertEquals(75_000, account.available(), "available after the stand-in approval");

        account.clear(new ClearingRecord("N70311", 25_000));
        assertEquals(75_000, account.ledger(), "ledger after clearing");
        assertEquals(75_000, account.available(), "available after clearing");
    }

    @Test
    void theFinalAmountIsWhatPosts() throws Exception {
        Account account = new Account(100_000);
        ISOMsg answer = account.authorize(message("0100", 18_000, "000613"));

        account.clear(new ClearingRecord(answer.getString(38), 15_240));

        assertEquals(84_760, account.ledger(), "the cardholder pays what the merchant cleared");
    }

    @Test
    void nothingStaysHeldAfterClearing() throws Exception {
        Account account = new Account(100_000);
        ISOMsg answer = account.authorize(message("0100", 18_000, "000614"));

        account.clear(new ClearingRecord(answer.getString(38), 15_240));

        assertEquals(account.ledger(), account.available(), "nothing is pending any more");
    }

    @Test
    void aClearingWithNoAuthorizationStillPosts() throws Exception {
        Account account = new Account(100_000);
        ISOMsg answer = account.authorize(message("0100", 12_000, "000615"));

        account.clear(new ClearingRecord("Y1", 3_500));    // approved offline by a terminal

        assertEquals(96_500, account.ledger(), "the offline purchase was paid for");
        assertEquals(84_500, account.available(), "the other purchase is still held");
        account.clear(new ClearingRecord(answer.getString(38), 12_000));
        assertEquals(84_500, account.available());
    }

    @Test
    void aStandInApprovalOverTheBalanceStillCounts() throws Exception {
        Account account = new Account(10_000);

        ISOMsg ack = account.standIn(standInAdvice(13_000, "000616", "N70412"));

        assertEquals("0130", ack.getMTI());
        assertEquals("00", ack.getString(39), "an advice is acknowledged");
        assertEquals(-3_000, account.available(), "the purchase happened, so it counts");
    }
}
