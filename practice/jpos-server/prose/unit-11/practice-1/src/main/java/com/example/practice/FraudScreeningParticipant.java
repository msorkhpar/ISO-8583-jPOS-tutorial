package com.example.practice;

import java.io.Serializable;
import java.math.BigDecimal;
import org.jpos.transaction.TransactionParticipant;

/** Refuses a bad amount, then screens the transaction with an external fraud service. */
public final class FraudScreeningParticipant implements TransactionParticipant {

    /** The external fraud check. */
    public interface FraudService {
        boolean isFraudulent(String cardNumber, BigDecimal amount);
    }

    public FraudScreeningParticipant(FraudService fraudService) {
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
