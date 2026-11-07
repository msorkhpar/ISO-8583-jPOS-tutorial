package com.example.practice;

import org.jpos.iso.ISOCurrency;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/** Approves a cash withdrawal only for an amount the ATM can pay out in its notes. */
public class CashWithdrawal implements TransactionParticipant {
    private static final int[] ECHOED = { 0, 2, 3, 4, 7, 11, 12, 13, 32, 37, 41, 42, 49, 70 };

    /** Per ISO 4217 numeric currency: the smallest note the ATM holds, and the most one withdrawal may take. */
    static final Map<String, BigDecimal[]> CASSETTES = Map.of(
            "978", new BigDecimal[] { new BigDecimal("10.00"), new BigDecimal("400.00") },     // euro
            "410", new BigDecimal[] { new BigDecimal("10000"), new BigDecimal("1000000") },    // won
            "414", new BigDecimal[] { new BigDecimal("5.000"), new BigDecimal("300.000") });   // Kuwaiti dinar

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        try {
            BigDecimal amount = amountOf(request);
            String rc = amount == null ? "30" : decide(amount, request.getString(49));
            ISOMsg response = (ISOMsg) request.clone(ECHOED);
            response.setResponseMTI();
            response.set(39, rc);
            ctx.put(ContextConstants.RESPONSE.toString(), response);
            return "00".equals(rc) ? PREPARED | NO_JOIN : ABORTED;
        } catch (ISOException e) {
            ctx.log(e);
            return ABORTED;
        }
    }

    /** The amount the cardholder asked for, in units of its currency; null when the request doesn't say. */
    static BigDecimal amountOf(ISOMsg request) {
        // TODO: Fields 4 and 49 of the request as an amount the rest of this class can compare
        //       with CASSETTES. Return null for any request that doesn't state one, so prepare()
        //       can answer it with 30. Terminals you don't control send these.
        throw new IllegalStateException("TODO: amountOf");
    }

    /** The response code for a readable amount: 00, 13 (not payable in notes), 61 (over the maximum) or 57 (no cassette). */
    static String decide(BigDecimal amount, String currency) {
        // TODO: 57 for a currency the ATM has no cassette for, 61 above that currency's maximum,
        //       13 for an amount its notes can't make, otherwise 00.
        throw new IllegalStateException("TODO: decide");
    }
}
