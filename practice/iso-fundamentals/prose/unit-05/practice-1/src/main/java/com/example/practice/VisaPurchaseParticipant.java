package com.example.practice;

import java.io.Serializable;
import org.jpos.transaction.TransactionParticipant;

/** The transaction step that approves Visa purchases. */
public class VisaPurchaseParticipant implements TransactionParticipant {

    public VisaPurchaseParticipant(String acceptorId, String nameAndLocation) {
    }

    /** Purchase, Cash Advance, Void or Refund, from a processing code; null for anything else. */
    public static String transactionType(String processingCode) {
        throw new UnsupportedOperationException("write transactionType");
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
