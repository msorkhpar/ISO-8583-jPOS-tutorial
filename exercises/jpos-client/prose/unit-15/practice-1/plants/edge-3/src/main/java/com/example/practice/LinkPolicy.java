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
        refusals++;
        notBefore = now + Math.min(firstWait << (refusals - 1), longestWait);
        return notBefore;
    }

    /** The host accepted a sign-on. */
    public synchronized void signedOn() {
        refusals = 0;
        notBefore = 0;
    }
}
