package com.example.practice;

import org.jpos.space.Space;
import org.jpos.space.SpaceUtil;

/**
 * One transaction in flight per terminal. A participant claims the terminal in prepare and
 * releases it when the transaction has ended; a request from a terminal that is already claimed
 * is declined. Every TransactionManager that runs such a participant has its own instance.
 */
public class TerminalLock {
    private final Space<String, Object> sp;
    private final long maxHold;                  // ms: longer than any transaction may run

    public TerminalLock(Space<String, Object> sp, long maxHold) {
        this.sp = sp;
        this.maxHold = maxHold;
    }

    /** Where a terminal's claim lives in the space. */
    static String key(String terminal) {
        return "inflight." + terminal;
    }

    /** True if this call claimed the terminal, false if a transaction already holds it. */
    public boolean claim(String terminal) {
        // TODO: claim the terminal for this transaction. Two sessions, or two TransactionManagers,
        //       may claim the same terminal at the same moment; exactly one may win. A session that
        //       hangs or dies never calls release.
        throw new IllegalStateException("TODO: claim");
    }

    /** Ends the claim, so that the terminal's next request is accepted at once. */
    public void release(String terminal) {
        // TODO: give the terminal back.
        throw new IllegalStateException("TODO: release");
    }
}
