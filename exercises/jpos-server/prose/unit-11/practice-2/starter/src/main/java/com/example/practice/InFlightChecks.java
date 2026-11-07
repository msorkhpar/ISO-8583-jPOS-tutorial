package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The team's checks for CardStatus, the participant that asks the card service about each
 * purchase and answers it. The tests run them against a real TransactionManager with two sessions,
 * whose chain is CardStatus and SendResponse. A check throws AssertionError when CardStatus gets
 * something wrong.
 */
public class InFlightChecks {

    /** One chain, as a check sees it. Every card is active. */
    public interface Chain {
        /** Sends a request as a terminal would. Returns its answer once the transaction has ended, or null. */
        ISOMsg send(ISOMsg request) throws Exception;

        /** Sends a request as a terminal would, and returns at once. */
        Pending start(ISOMsg request) throws Exception;

        /** The next card-service lookup, whichever transaction makes it, waits until releaseLookup. */
        void holdNextLookup();

        /** Returns once a lookup is being held. */
        void awaitHeldLookup() throws Exception;

        /** Lets the held lookup answer. */
        void releaseLookup();
    }

    /** A request that has been sent. */
    public interface Pending {
        /** Its answer once the transaction has ended, or null. */
        ISOMsg answer() throws Exception;
    }

    /** Runs every check, each on a fresh chain. */
    public static void checkAll(Supplier<Chain> chains) throws Exception {
        onePurchase(chains.get());
        twoCardholdersAtOnce(chains.get());
    }

    /** One purchase is approved, and its answer is about that purchase. */
    static void onePurchase(Chain chain) throws Exception {
        ISOMsg request = purchase("4000000000000069", "29110060", "000000002750");
        ISOMsg answer = chain.send(request);
        expect("00", answer == null ? null : answer.getString(39), "one purchase: field 39");
        for (int field : new int[] { 2, 4, 11, 41 })
            expect(request.getString(field), answer.getString(field), "one purchase: field " + field);
    }

    /** Two cardholders at two terminals, whose purchases are in the server at the same time. */
    static void twoCardholdersAtOnce(Chain chain) throws Exception {
        // TODO: Two cardholders pay at two terminals, and both purchases are in the server at the
        //       same moment. The check must fail for a CardStatus that mixes anything of one purchase
        //       into the other's answer, and pass for one that keeps them apart.
        throw new IllegalStateException("TODO: twoCardholdersAtOnce");
    }

    // ---- helpers ----

    private static int stan;

    /** A purchase with the next STAN. */
    static synchronized ISOMsg purchase(String card, String terminal, String amount) throws ISOException {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, card);
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, String.format("%06d", ++stan));
        m.set(41, terminal);
        return m;
    }

    /** Fails the check unless the two values are equal. */
    static void expect(Object expected, Object actual, String what) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError(what + ": expected " + expected + " but was " + actual);
    }
}
