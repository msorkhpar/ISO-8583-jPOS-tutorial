package com.example.practice;

import org.jpos.space.Space;
import org.jpos.space.TSpace;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalLockTest {
    static final long HOLD = 45000;

    @Test
    void aBusyTerminalIsRefusedAndOthersAreNot() {
        Probe sp = new Probe();
        TerminalLock lock = new TerminalLock(sp, HOLD);
        assertTrue(lock.claim("30570012"), "a free terminal can be claimed");
        assertFalse(lock.claim("30570012"), "a terminal with a transaction in flight is refused");
        assertTrue(lock.claim("30570044"), "another terminal is not affected");
    }

    @Test
    void aClaimEndsOnItsOwnIfNobodyReleasesIt() {
        Probe sp = new Probe();
        new TerminalLock(sp, HOLD).claim("30570013");
        assertEquals(HOLD, sp.lastLease.get(),
            "the claim must expire after maxHold ms, or a hung session locks the terminal for good");
    }

    @Test
    void twoTransactionManagersClaimingAtOnceGetOneClaim() throws Exception {
        Probe sp = new Probe();
        sp.overlapReads = true;
        TerminalLock purchases = new TerminalLock(sp, HOLD);   // one instance per TransactionManager
        TerminalLock refunds = new TerminalLock(sp, HOLD);
        AtomicInteger won = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);
        Thread a = new Thread(() -> { await(go); if (purchases.claim("30570014")) won.incrementAndGet(); });
        Thread b = new Thread(() -> { await(go); if (refunds.claim("30570014")) won.incrementAndGet(); });
        a.start(); b.start();
        go.countDown();
        a.join(10000); b.join(10000);
        assertEquals(1, won.get(), "exactly one of two simultaneous claims may win");
    }

    @Test
    void aReleasedTerminalCanBeClaimedAgainAtOnce() {
        Probe sp = new Probe();
        TerminalLock lock = new TerminalLock(sp, HOLD);
        assertTrue(lock.claim("30570015"));
        lock.release("30570015");
        assertTrue(lock.claim("30570015"), "after release the next request is accepted");
    }

    private static void await(CountDownLatch l) {
        try { l.await(); } catch (InterruptedException e) { throw new IllegalStateException(e); }
    }

    /** A TSpace that records leases and, when asked, makes two readers of rdp meet after reading. */
    static final class Probe implements Space<String, Object> {
        final TSpace<String, Object> sp = new TSpace<>();
        final AtomicInteger lastLease = new AtomicInteger(-1);
        final CyclicBarrier readers = new CyclicBarrier(2);
        volatile boolean overlapReads;

        private void lease(long t) { lastLease.set((int) t); }

        @Override public Object rdp(String k) {
            Object v = sp.rdp(k);                                // both read before either goes on
            if (overlapReads) {
                try { readers.await(1, TimeUnit.SECONDS); }      // wait for the other reader, briefly
                catch (TimeoutException | BrokenBarrierException e) { /* alone: go on */ }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            return v;
        }
        @Override public void out(String k, Object v) { lease(0); sp.out(k, v); }
        @Override public void out(String k, Object v, long t) { lease(t); sp.out(k, v, t); }
        @Override public void push(String k, Object v) { lease(0); sp.push(k, v); }
        @Override public void push(String k, Object v, long t) { lease(t); sp.push(k, v, t); }
        @Override public void put(String k, Object v) { lease(0); sp.put(k, v); }
        @Override public void put(String k, Object v, long t) { lease(t); sp.put(k, v, t); }
        @Override public Object in(String k) { return sp.in(k); }
        @Override public Object rd(String k) { return sp.rd(k); }
        @Override public Object in(String k, long t) { return sp.in(k, t); }
        @Override public Object rd(String k, long t) { return sp.rd(k, t); }
        @Override public Object inp(String k) { return sp.inp(k); }
        @Override public void nrd(String k) { sp.nrd(k); }
        @Override public Object nrd(String k, long t) { return sp.nrd(k, t); }
        @Override public boolean existAny(String[] ks) { return sp.existAny(ks); }
        @Override public boolean existAny(String[] ks, long t) { return sp.existAny(ks, t); }
    }
}
