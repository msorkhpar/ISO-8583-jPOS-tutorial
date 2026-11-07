package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.util.HashMap;
import java.util.Map;

/**
 * A credit card account as the issuer's authorization host keeps it. Amounts are in minor units
 * (cents). Holds are kept by the approval code that went out in field 38.
 */
public class Account {
    private long ledger;
    private final Map<String, Long> holds = new HashMap<>();
    private int codes;

    public Account(long ledger) {
        this.ledger = ledger;
    }

    /** The money posted to the account. */
    public synchronized long ledger() {
        return ledger;
    }

    /** What the cardholder can still spend: the ledger minus every open hold. */
    public synchronized long available() {
        long held = 0;
        for (long h : holds.values())
            held += h;
        return ledger - held;
    }

    /** An 0100 from the network. Returns the 0110. */
    public synchronized ISOMsg authorize(ISOMsg request) throws ISOException {
        long amount = Long.parseLong(request.getString(4));
        if (amount > available())
            return answer(request, "51", null);
        String code = String.format("I%05d", ++codes);
        holds.put(code, amount);
        return answer(request, "00", code);
    }

    /**
     * An 0120 from the network: it approved this purchase while our host did not answer.
     * Field 38 holds the approval code the network gave out. Returns the answer to send back.
     */
    public synchronized ISOMsg standIn(ISOMsg advice) throws ISOException {
        holds.put(advice.getString(38), Long.parseLong(advice.getString(4)));
        return answer(advice, "00", null);
    }

    /** A clearing record from the network's file: the final amount of one purchase. */
    public synchronized void clear(ClearingRecord record) {
        holds.computeIfPresent(record.approvalCode(), (k, h) -> h - record.amount());
        ledger -= record.amount();
    }

    /** One line of the clearing file. */
    public record ClearingRecord(String approvalCode, long amount) { }

    static ISOMsg answer(ISOMsg in, String rc, String approvalCode) throws ISOException {
        ISOMsg r = (ISOMsg) in.clone();
        r.setResponseMTI();
        r.set(39, rc);
        if (approvalCode != null)
            r.set(38, approvalCode);
        return r;
    }
}
