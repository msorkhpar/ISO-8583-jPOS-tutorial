package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;

/**
 * Advices (0220 completions) that must reach the host: one sender, oldest first, kept in the
 * {@link Store} until the host has answered them.
 */
public class AdviceSaf {
    private final Store store;

    public AdviceSaf(Store store) {
        this.store = store;
    }

    public void add(ISOMsg advice) {
        store.insert(advice);
    }

    public boolean isEmpty() {
        return store.oldest() == null;
    }

    /** One attempt for the oldest advice. True when the host has answered it. */
    public synchronized boolean sendOldest(MUX mux, long timeout) throws ISOException {
        Store.Row row = next();
        if (row == null || !mux.isConnected())
            return false;
        ISOMsg answer = mux.request(row.msg(), timeout);
        if (answer != null) {
            if (!"00".equals(answer.getString(39)))
                return false;
            answered(row);
            return true;
        }
        notAnswered(row);
        return false;
    }

    /** The advice to send now. */
    Store.Row next() {
        return store.oldest();
    }

    /** The host has seen it. */
    void answered(Store.Row row) {
        store.delete(row.id());
    }

    /** The attempt went out and got no answer. */
    void notAnswered(Store.Row row) throws ISOException {
        ISOMsg advice = row.msg();
        if (!advice.isRetransmission()) {
            advice = (ISOMsg) advice.clone();
            advice.setRetransmissionMTI();
        }
        store.update(row.id(), advice, row.attempts());
    }

    /**
     * Given: a table of messages, as a database would keep them. Row ids grow, and the oldest row
     * is the one with the lowest id. It outlives any AdviceSaf that uses it, the way a database
     * outlives a process.
     */
    public static final class Store {
        public record Row(long id, ISOMsg msg, int attempts) {}

        private final java.util.TreeMap<Long, Row> rows = new java.util.TreeMap<>();
        private long lastId;

        public synchronized long insert(ISOMsg msg) {
            long id = ++lastId;
            rows.put(id, new Row(id, msg, 0));
            return id;
        }

        /** The oldest row, or null. The row stays in the table. */
        public synchronized Row oldest() {
            return rows.isEmpty() ? null : rows.firstEntry().getValue();
        }

        /** Changes a row in place: same id, so the same place in the line. */
        public synchronized void update(long id, ISOMsg msg, int attempts) {
            rows.put(id, new Row(id, msg, attempts));
        }

        public synchronized void delete(long id) {
            rows.remove(id);
        }

        public synchronized java.util.List<ISOMsg> all() {
            return rows.values().stream().map(Row::msg).toList();
        }
    }
}
