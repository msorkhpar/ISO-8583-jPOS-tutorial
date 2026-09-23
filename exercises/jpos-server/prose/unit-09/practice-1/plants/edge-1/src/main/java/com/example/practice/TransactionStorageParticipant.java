package com.example.practice;

import java.io.Serializable;
import java.math.BigDecimal;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

/** Stores every transaction that passes through the transaction manager. */
public final class TransactionStorageParticipant implements TransactionParticipant {

    /** One stored transaction. */
    public record StoredTransaction(String transactionId, BigDecimal amount, String currency,
            String cardNumber, String transactionType, String status) {
    }

    /** The service a transaction is saved through. */
    public interface TransactionStore {
        void save(StoredTransaction transaction);
    }

    private final TransactionStore store;

    public TransactionStorageParticipant(TransactionStore store) {
        this.store = store;
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        BigDecimal amount = new BigDecimal((String) ctx.get("amount"));
        store.save(new StoredTransaction(
                (String) ctx.get("transactionId"), amount, (String) ctx.get("currency"),
                (String) ctx.get("cardNumber"), (String) ctx.get("transactionType"),
                (String) ctx.get("status")));
        return PREPARED | NO_JOIN | READONLY;
    }

    @Override
    public void commit(long id, Serializable context) {
    }

    @Override
    public void abort(long id, Serializable context) {
    }
}
