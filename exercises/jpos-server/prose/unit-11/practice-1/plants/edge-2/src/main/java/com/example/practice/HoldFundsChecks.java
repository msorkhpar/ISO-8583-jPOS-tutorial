package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The team's checks for HoldFunds, the participant that holds the purchase amount on a prepaid
 * card while the issuer decides. The tests run them against a real TransactionManager whose chain
 * is HoldFunds, an issuer stand-in and SendResponse. A check throws AssertionError when HoldFunds
 * gets something wrong.
 */
public class HoldFundsChecks {

    /** One chain with fresh balances, as a check sees it. */
    public interface Chain {
        /** Sets the card's prepaid balance, in minor units. */
        void open(String card, long balance);

        /** The card's balance now, in minor units. */
        long balance(String card);

        /** What the issuer stand-in answers in field 39 from now on. A fresh chain answers "00". */
        void issuerAnswers(String responseCode);

        /** Sends a request as a terminal would. Returns its answer once the transaction has ended, or null. */
        ISOMsg send(ISOMsg request) throws Exception;
    }

    /** Runs every check, each on a fresh chain. */
    public static void checkAll(Supplier<Chain> chains) throws Exception {
        approvedPurchase(chains.get());
        purchaseTheIssuerDeclines(chains.get());
        purchaseOverTheBalance(chains.get());
    }

    /** An approved purchase is answered 00, and the card keeps the rest of its balance. */
    static void approvedPurchase(Chain chain) throws Exception {
        chain.open("4000000000000002", 20000);
        ISOMsg answer = chain.send(purchase("4000000000000002", "000000007500"));
        expect("00", answer == null ? null : answer.getString(39), "approved purchase: field 39");
        expect(12500L, chain.balance("4000000000000002"), "approved purchase: balance");
    }

    /** A purchase the card could pay, which the issuer declines with 05. */
    static void purchaseTheIssuerDeclines(Chain chain) throws Exception {
        chain.open("4000000000000010", 20000);
        chain.issuerAnswers("05");
        ISOMsg answer = chain.send(purchase("4000000000000010", "000000007500"));
        expect("05", answer == null ? null : answer.getString(39), "issuer decline: field 39");
        expect(20000L, chain.balance("4000000000000010"), "issuer decline: balance");
    }

    /** A purchase larger than the card's balance, which HoldFunds refuses with 51. */
    static void purchaseOverTheBalance(Chain chain) throws Exception {
        chain.open("4000000000000028", 5000);
        ISOMsg answer = chain.send(purchase("4000000000000028", "000000007500"));
        expect("51", answer == null ? null : answer.getString(39), "refusal: field 39");
    }

    // ---- helpers ----

    private static int stan;

    /** A purchase from terminal 29110051, with the next STAN. */
    static synchronized ISOMsg purchase(String card, String amount) throws ISOException {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, card);
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, String.format("%06d", ++stan));
        m.set(41, "29110051");
        return m;
    }

    /** Fails the check unless the two values are equal. */
    static void expect(Object expected, Object actual, String what) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError(what + ": expected " + expected + " but was " + actual);
    }
}
