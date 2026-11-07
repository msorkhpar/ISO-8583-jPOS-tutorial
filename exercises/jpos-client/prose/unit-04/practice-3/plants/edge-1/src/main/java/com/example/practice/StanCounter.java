package com.example.practice;

import org.jpos.iso.ISOUtil;

/**
 * STANs for one outbound link, numbered from a row in a database table that every gateway process
 * on the link shares.
 */
public class StanCounter {
    private final Table table;
    private final String link;
    private final java.util.concurrent.atomic.AtomicLong issued = new java.util.concurrent.atomic.AtomicLong();

    public StanCounter(Table table, String link) {
        this.table = table;
        this.link = link;
    }

    /** The STAN of the next message on this link. */
    public String next() {
        long n = issued.getAndIncrement();      // fast: no round trip to the table
        return ISOUtil.zeropad((n % 999999) + 1, 6);
    }

    /**
     * Given: one counter row per link, as a database keeps it. It outlives every StanCounter, the
     * way a database outlives a process.
     */
    public static class Table {
        private final java.util.Map<String, Long> rows = new java.util.HashMap<>();

        /** SELECT value FROM stan WHERE link = ?  (0 when there is no row yet) */
        public synchronized long read(String link) {
            return rows.getOrDefault(link, 0L);
        }

        /** UPDATE stan SET value = ? WHERE link = ? */
        public synchronized void write(String link, long value) {
            rows.put(link, value);
        }

        /** UPDATE stan SET value = next WHERE link = ? AND value = expected; true if a row changed */
        public synchronized boolean compareAndSet(String link, long expected, long next) {
            if (rows.getOrDefault(link, 0L) != expected)
                return false;
            rows.put(link, next);
            return true;
        }
    }
}
