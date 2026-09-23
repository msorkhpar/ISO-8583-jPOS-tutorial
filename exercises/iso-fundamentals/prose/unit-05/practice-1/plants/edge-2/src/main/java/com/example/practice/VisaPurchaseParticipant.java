package com.example.practice;

import java.io.Serializable;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

/** The transaction step that approves Visa purchases. */
public class VisaPurchaseParticipant implements TransactionParticipant {

    private final String acceptorId;
    private final String nameAndLocation;

    public VisaPurchaseParticipant(String acceptorId, String nameAndLocation) {
        this.acceptorId = acceptorId;
        this.nameAndLocation = nameAndLocation;
    }

    /** Purchase, Cash Advance, Void or Refund, from a processing code; null for anything else. */
    public static String transactionType(String processingCode) {
        if (processingCode == null || processingCode.length() < 2) {
            return null;
        }
        switch (processingCode.substring(0, 2)) {
            case "00": return "Purchase";
            case "01": return "Cash Advance";
            case "02": return "Void";
            case "20": return "Refund";
            default: return null;
        }
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = (ISOMsg) ctx.get("REQUEST");
        try {
            if (!"0100".equals(request.getMTI())
                    || !"Purchase".equals(transactionType(request.getString(3)))) {
                ctx.put("RESULT", "Invalid transaction type");
                return ABORTED;
            }
            ISOMsg response = request;
            response.setMTI("0110");
            response.set(39, "00");
            response.set(42, ISOUtil.padright(acceptorId, 15, ' '));
            response.set(43, ISOUtil.padright(nameAndLocation, 40, ' '));
            ctx.put("RESPONSE", response);
            return PREPARED;
        } catch (ISOException e) {
            ctx.put("RESULT", "Invalid transaction type");
            return ABORTED;
        }
    }

    @Override
    public void commit(long id, Serializable context) {
    }

    @Override
    public void abort(long id, Serializable context) {
    }
}
