package com.example.practice;

/**
 * Polled every few seconds with a TransactionManager's head and tail (getHead(), getTail()), and
 * the time of the poll. Names a transaction that has stopped finishing, so that someone is paged
 * before the server runs out of sessions.
 */
public class StuckWatch {
    /** No transaction on this server legitimately takes longer than this. */
    private final long limitMillis;
    private long watchedTail = -1;
    private long since;

    public StuckWatch(long limitMillis) {
        this.limitMillis = limitMillis;
    }

    /** The id of a transaction that has been running for longer than the limit, or -1. */
    public long poll(long head, long tail, long nowMillis) {
        if (tail >= head) {                                 // nothing unfinished
            watchedTail = -1;
            return -1;
        }
        if (watchedTail == -1) {                            // something unfinished, from now
            watchedTail = tail;
            since = nowMillis;
        }
        return nowMillis - since > limitMillis ? tail : -1;
    }
}
