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
        // TODO: Some calls should not even try. Which, and what do they return? (one line)
        ISOMsg answer = mux.request(row.msg(), timeout);
        if (answer != null) {
            answered(row);
            return true;
        }
        notAnswered(row);
        return false;
    }

    /** The advice to send now. */
    Store.Row next() {
        // TODO: The oldest advice. The process may die while it is being sent. (one line)
        throw new IllegalStateException("TODO: next");
    }

    /** The host has seen it. */
    void answered(Store.Row row) {
        // TODO: Whatever field 39 says. (one line)
        throw new IllegalStateException("TODO: answered");
    }

    /** The attempt went out and got no answer. */
    void notAnswered(Store.Row row) throws ISOException {
        // TODO: The next attempt is a repeat, and this is still the oldest advice. (about 5 lines)
        throw new IllegalStateException("TODO: notAnswered");
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
