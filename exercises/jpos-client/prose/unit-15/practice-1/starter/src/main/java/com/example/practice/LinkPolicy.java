package com.example.practice;

/**
 * What the sign-on manager asks before it signs one link on, and tells after each attempt.
 * Times are milliseconds, passed in by the caller.
 */
public class LinkPolicy {
    private final long firstWait;
    private final long longestWait;
    private int refusals;
    private long notBefore;

    public LinkPolicy(long firstWait, long longestWait) {
        this.firstWait = firstWait;
        this.longestWait = longestWait;
    }

    /** May the manager send a sign-on at this time? */
    public synchronized boolean maySignOn(long now) {
        return now >= notBefore;
    }

    /**
     * The sign-on sent at {@code now} was refused, or got no answer. Returns the earliest time of
     * the next attempt.
     */
    public synchronized long refused(long now) {
        // TODO: wait longer after each refusal in a row, but never longer than longestWait,
        //       however long the host keeps refusing. Return when the next attempt may go out.
        throw new IllegalStateException("TODO: refused");
    }

    /** The host accepted a sign-on. */
    public synchronized void signedOn() {
        // TODO: the next refusal is the first of a new series, and nothing stops a sign-on now.
        throw new IllegalStateException("TODO: signedOn");
    }
}
