package com.example.practice;

/**
 * What the sign-on manager asks before it signs one link on, and tells after each attempt and
 * each echo. Times are milliseconds, passed in by the caller.
 */
public class LinkPolicy {
    private final long firstWait;
    private final long longestWait;
    private final int echoLimit;
    private int refusals;
    private long notBefore;
    private int echoFailures;

    public enum EchoVerdict { KEEP, SIGN_ON_AGAIN }

    public LinkPolicy(long firstWait, long longestWait, int echoLimit) {
        this.firstWait = firstWait;
        this.longestWait = longestWait;
        this.echoLimit = echoLimit;
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
        long wait = firstWait;
        for (int i = 1; i < refusals && wait < longestWait; i++)
            wait = Math.min(wait * 2, longestWait);
        notBefore = now + Math.min(wait, longestWait);
        return notBefore;
    }

    /** The host accepted a sign-on. */
    public synchronized void signedOn() {
        refusals = 0;
        notBefore = 0;
        echoFailures = 0;
    }

    /** An echo on the signed-on link was answered with 00. */
    public synchronized void echoAnswered() {
        echoFailures = 0;
    }

    /** An echo on the signed-on link got no answer, or a refusal. Should the link stay in the pool? */
    public synchronized EchoVerdict echoFailed() {
        echoFailures++;
        return echoFailures >= echoLimit ? EchoVerdict.SIGN_ON_AGAIN : EchoVerdict.KEEP;
    }
}
