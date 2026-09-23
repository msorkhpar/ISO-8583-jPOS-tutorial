package com.example.practice;

import java.io.Serializable;
import java.math.BigDecimal;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

/** Refuses a bad amount, then screens the transaction with an external fraud service. */
public final class FraudScreeningParticipant implements TransactionParticipant {

    /** The external fraud check. */
    public interface FraudService {
        boolean isFraudulent(String cardNumber, BigDecimal amount);
    }

    private final FraudService fraudService;

    public FraudScreeningParticipant(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        String text = (String) ctx.get("amount");
        if (text == null || text.isEmpty()) {
            ctx.put("error", "Amount is missing");
            return ABORTED;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(text);
        } catch (NumberFormatException e) {
            ctx.put("error", "Invalid amount format");
            return ABORTED;
        }
        if (amount.signum() <= 0) {
            ctx.put("error", "Amount must be positive");
            return ABORTED;
        }
        boolean fraudulent;
        try {
            fraudulent = fraudService.isFraudulent((String) ctx.get("cardNumber"), amount);
        } catch (RuntimeException e) {
            ctx.put("error", "Fraud check unavailable");
            return ABORTED;
        }
        if (fraudulent) {
            ctx.put("fraudDetected", Boolean.TRUE);
            return ABORTED;
        }
        return PREPARED;
    }

    @Override
    public void commit(long id, Serializable context) {
    }

    @Override
    public void abort(long id, Serializable context) {
    }
}
