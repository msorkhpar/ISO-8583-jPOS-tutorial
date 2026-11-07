package com.example.practice;

/**
 * Polled every few seconds with a TransactionManager's head and tail (getHead(), getTail()), and
 * the time of the poll. Names a transaction that has stopped finishing, so that someone is paged
 * before the server runs out of sessions.
 */
public class StuckWatch {
    /** No transaction on this server legitimately takes longer than this. */
    private final long limitMillis;

    public StuckWatch(long limitMillis) {
        this.limitMillis = limitMillis;
    }

    /** The id of a transaction that has been running for longer than the limit, or -1. */
    public long poll(long head, long tail, long nowMillis) {
        // TODO: Decide from what head and tail say, and from what earlier polls saw. Add the fields
        //       you need: one instance is polled for the life of the server.
        throw new IllegalStateException("TODO: poll");
    }
}
