package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.junit.jupiter.api.Test;

class TransactionStorageParticipantTest {

    private final List<TransactionStorageParticipant.StoredTransaction> saved = new ArrayList<>();
    private final TransactionStorageParticipant participant =
            new TransactionStorageParticipant(saved::add);

    private static Context purchase(String amount) {
        Context ctx = new Context();
        ctx.put("transactionId", "TXN-1");
        if (amount != null) {
            ctx.put("amount", amount);
        }
        ctx.put("currency", "EUR");
        ctx.put("cardNumber", "4000000000000002");
        ctx.put("transactionType", "PURCHASE");
        ctx.put("status", "APPROVED");
        return ctx;
    }

    @Test
    void aTransactionIsStoredOnceAndTheParticipantStaysOutOfTheCommit() {
        int answer = participant.prepare(1L, purchase("100.50"));
        assertEquals(TransactionParticipant.PREPARED | TransactionParticipant.NO_JOIN
                | TransactionParticipant.READONLY, answer);
        assertEquals(List.of(new TransactionStorageParticipant.StoredTransaction(
                "TXN-1", new BigDecimal("100.50"), "EUR", "4000000000000002",
                "PURCHASE", "APPROVED")), saved);
    }

    @Test
    void anAmountThatIsNotANumberAbortsAndStoresNothing() {
        assertEquals(TransactionParticipant.ABORTED, participant.prepare(1L, purchase("12,50")));
        assertEquals(TransactionParticipant.ABORTED, participant.prepare(2L, purchase(null)));
        assertTrue(saved.isEmpty());
    }
}
