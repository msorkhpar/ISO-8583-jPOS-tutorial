package com.example.practice;

import java.io.Serializable;
import java.math.BigDecimal;
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

    public TransactionStorageParticipant(TransactionStore store) {
    }

    @Override
    public int prepare(long id, Serializable context) {
        throw new UnsupportedOperationException("write prepare");
    }

    @Override
    public void commit(long id, Serializable context) {
    }

    @Override
    public void abort(long id, Serializable context) {
    }
}
