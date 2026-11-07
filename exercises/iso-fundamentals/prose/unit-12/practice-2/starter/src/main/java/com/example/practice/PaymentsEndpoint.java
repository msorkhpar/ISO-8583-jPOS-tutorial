package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** What the gateway does for POST /payments: one 0200 to the acquirer's host per payment. */
public final class PaymentsEndpoint {

    /** The JSON body of a payment, already mapped (the card is a test PAN, the amount in USD). */
    public record Payment(String card, BigDecimal amount, String terminal) {
    }

    /**
     * What the shop is told. Status is APPROVED (with the approval code), DECLINED, FAILED (no answer
     * from the host; the purchase was reversed), REFUSED (the call can't be accepted) or IN_PROGRESS
     * (another call with the same key is still waiting for the host).
     */
    public record Result(String status, String approvalCode) {
    }

    /** The acquirer's host: the response, or null when none came in time. */
    public interface Host {
        ISOMsg request(ISOMsg m) throws ISOException;
    }

    /** Thrown by {@link KeyTable#insert} when the key is already in the table. */
    public static final class DuplicateKeyException extends Exception {
        DuplicateKeyException(String key) {
            super("duplicate key " + key);
        }
    }

    /** A database table of idempotency keys, with a unique constraint on the key column. */
    public static final class KeyTable {
        /** One row: the payment the key was first used for, and its result once known (null until then). */
        public record Row(Payment payment, Result result) {
        }

        private final Map<String, Row> rows = new HashMap<>();

        public synchronized void insert(String key, Payment payment) throws DuplicateKeyException {
            if (rows.containsKey(key))
                throw new DuplicateKeyException(key);
            rows.put(key, new Row(payment, null));
        }

        public synchronized Optional<Row> find(String key) {
            return Optional.ofNullable(rows.get(key));
        }

        /** Stores the result of the row's payment; insert the row first. */
        public synchronized void complete(String key, Result result) {
            Row row = rows.get(key);
            if (row == null)
                throw new IllegalStateException("no row for key " + key);
            rows.put(key, new Row(row.payment(), result));
        }

        public synchronized void delete(String key) {
            rows.remove(key);
        }
    }

    private final Host host;
    private final KeyTable keys;
    private final List<ISOMsg> reversals = Collections.synchronizedList(new ArrayList<>());
    private int stan;

    public PaymentsEndpoint(Host host, KeyTable keys) {
        this.host = host;
        this.keys = keys;
    }

    /** The shop's call: {@code key} is its Idempotency-Key header. */
    public Result pay(String key, Payment p) throws ISOException {
        // TODO: shops call again with the same key whenever their HTTP client times out, sometimes
        //       twice at once, and now and then reuse a key by mistake. Whatever the calls, a
        //       payment reaches the host at most once, and every call tells the shop the truth
        //       about it. The key table, purchase, reversalOf and outcomeOf are ready to use.
        throw new IllegalStateException("TODO: pay");
    }

    /** Reversals waiting to be delivered to the host by the store-and-forward sender. */
    public List<ISOMsg> reversals() {
        synchronized (reversals) {
            return new ArrayList<>(reversals);
        }
    }

    /** A new 0200 for the payment, with the link's next STAN. */
    synchronized ISOMsg purchase(Payment p) throws ISOException {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, p.card());
        m.set(3, "000000");
        m.set(4, ISOUtil.zeropad(p.amount().movePointRight(2).toBigIntegerExact().toString(), 12));
        m.set(11, ISOUtil.zeropad(++stan, 6));
        m.set(41, p.terminal());
        m.set(49, "840");
        return m;
    }

    /** The 0420 that undoes {@code original} if the host approved it. */
    synchronized ISOMsg reversalOf(ISOMsg original) throws ISOException {
        ISOMsg rev = (ISOMsg) original.clone(new int[] { 2, 3, 4, 41, 49 });
        rev.setMTI("0420");
        rev.set(11, ISOUtil.zeropad(++stan, 6));
        rev.set(90, original.getMTI() + original.getString(11) + ISOUtil.zeropad("", 32));
        return rev;
    }

    /** The shop's result for the host's answer. */
    static Result outcomeOf(ISOMsg response) {
        return "00".equals(response.getString(39))
                ? new Result("APPROVED", response.getString(38))
                : new Result("DECLINED", null);
    }
}
