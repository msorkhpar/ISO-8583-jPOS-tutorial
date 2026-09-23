package com.example.practice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

/** Identifies a request's card network and checks it against that network's required fields. */
public class CardNetworkCheck implements TransactionParticipant {

    private static final int[] VISA_FIELDS = {42, 43, 48};
    private static final int[] MASTERCARD_FIELDS = {48, 61, 63};

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg msg = ctx.get("REQUEST");
        String type = cardType(msg.getString(2));
        ctx.put("CARD_TYPE", type);
        if (type.equals("VISA")) {
            check(ctx, msg, "VISA", VISA_FIELDS);
        } else if (type.equals("MASTERCARD")) {
            check(ctx, msg, "MASTERCARD", MASTERCARD_FIELDS);
        }
        return PREPARED | NO_JOIN | READONLY;
    }

    private static String cardType(String pan) {
        if (pan.startsWith("4")) {
            return "VISA";
        }
        if (pan.length() >= 2 && pan.charAt(0) == '5' && pan.charAt(1) >= '1' && pan.charAt(1) <= '5') {
            return "MASTERCARD";
        }
        return "UNKNOWN";
    }

    private static void check(Context ctx, ISOMsg msg, String network, int[] required) {
        List<Integer> missing = new ArrayList<>();
        for (int field : required) {
            if (!msg.hasField(field)) {
                missing.add(field);
            }
        }
        ctx.put(network + "_TRANSACTION_VALID", missing.isEmpty());
        if (!missing.isEmpty()) {
            ctx.put(network + "_ERROR", "Missing " + network + " fields: " + missing);
        }
    }
}
