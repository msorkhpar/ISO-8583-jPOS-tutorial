package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionConstants;
import org.jpos.transaction.TransactionParticipant;
import org.junit.jupiter.api.Test;

class NetworkRoutingTest {

    /**
     * Plays the transaction manager's part as jPOS 2.1.7 does for plain participants:
     * prepare in order, stop at the first vote without PREPARED, then commit or abort
     * every participant that joined (every one whose vote lacked NO_JOIN).
     */
    private static String run(Context context) {
        List<TransactionParticipant> flow = List.of(
            new NetworkRouting.Validator(),
            new NetworkRouting.NetworkParticipant("VISA", "4"),
            new NetworkRouting.NetworkParticipant("MASTERCARD", "5"));
        List<TransactionParticipant> joined = new ArrayList<>();
        boolean aborted = false;
        for (TransactionParticipant participant : flow) {
            int vote = participant.prepare(1L, context);
            if ((vote & TransactionConstants.NO_JOIN) == 0) {
                joined.add(participant);
            }
            if ((vote & TransactionConstants.PREPARED) == 0) {
                aborted = true;
                break;
            }
        }
        for (TransactionParticipant participant : joined) {
            if (aborted) {
                participant.abort(1L, context);
            } else {
                participant.commit(1L, context);
            }
        }
        return aborted ? "ABORTED" : "COMMITTED";
    }

    private static Context transaction(String amount, String pan) {
        Context context = new Context();
        if (amount != null) {
            context.put("amount", amount);
        }
        if (pan != null) {
            context.put("pan", pan);
        }
        return context;
    }

    @Test
    void visaAndMastercardCardsBothCommitThroughTheSameFlow() {
        Context visa = transaction("100.00", "4111111111111111");
        assertEquals("COMMITTED", run(visa));
        assertEquals("VISA", visa.getString("network"));
        assertNull(visa.get("error"));

        Context mastercard = transaction("200.00", "5555555555554444");
        assertEquals("COMMITTED", run(mastercard));
        assertEquals("MASTERCARD", mastercard.getString("network"));
        assertNull(mastercard.get("error"));
    }

    @Test
    void anIncompleteTransactionIsAbortedBeforeAnyNetworkSeesIt() {
        Context noAmount = transaction(null, "4111111111111111");
        assertEquals("ABORTED", run(noAmount));
        assertNotNull(noAmount.get("error"));
        assertNull(noAmount.get("network"));
        assertNull(noAmount.get("sent"));

        Context noPan = transaction("100.00", null);
        assertEquals("ABORTED", run(noPan));
        assertNotNull(noPan.get("error"));
    }

    @Test
    void onlyTheCardsOwnNetworkIsCommitted() {
        Context visa = transaction("100.00", "4111111111111111");
        run(visa);
        assertEquals(List.of("VISA"), visa.get("sent"));

        Context mastercard = transaction("200.00", "5555555555554444");
        run(mastercard);
        assertEquals(List.of("MASTERCARD"), mastercard.get("sent"));
    }
}
