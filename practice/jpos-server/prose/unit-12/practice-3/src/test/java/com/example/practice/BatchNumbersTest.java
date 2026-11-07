package com.example.practice;

import com.example.practice.BatchNumbers.Profile;
import org.jpos.space.Space;
import org.jpos.space.TSpace;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchNumbersTest {

    @Test
    void eachSettlementOpensTheNextBatch() {
        TSpace<String, Object> sp = new TSpace<>();
        sp.put("profile.30570031", new Profile("30570031", "000000004417702", 7));
        BatchNumbers batches = new BatchNumbers(sp);
        assertEquals(8, batches.nextBatch("30570031"));
        assertEquals(9, batches.nextBatch("30570031"));
        assertEquals(10, batches.nextBatch("30570031"));
        assertEquals("000000004417702", batches.profile("30570031").merchant);
    }

    @Test
    void theNewNumberIsStoredInAPersistentSpaceToo() {
        Copying sp = new Copying();
        sp.put("profile.30570032", new Profile("30570032", "000000004417702", 41));
        BatchNumbers batches = new BatchNumbers(sp);
        assertEquals(42, batches.nextBatch("30570032"));
        assertEquals(43, batches.nextBatch("30570032"), "the second settlement starts from the stored 42");
        assertEquals(43, batches.profile("30570032").batch);
    }

    @Test
    void theTerminalKeepsOneProfile() {
        TSpace<String, Object> sp = new TSpace<>();
        sp.put("profile.30570033", new Profile("30570033", "000000004417702", 1));
        BatchNumbers batches = new BatchNumbers(sp);
        batches.nextBatch("30570033");
        batches.nextBatch("30570033");
        assertEquals(1, sp.size("profile.30570033"), "one profile per terminal, not one per settlement");
    }

    @Test
    void twoSessionsSettlingAtOnceGetTwoNumbers() throws Exception {
        Copying sp = new Copying();
        sp.put("profile.30570034", new Profile("30570034", "000000004417702", 300));
        sp.overlapReads = true;
        BatchNumbers batches = new BatchNumbers(sp);
        Set<Integer> got = ConcurrentHashMap.newKeySet();
        CountDownLatch go = new CountDownLatch(1);
        Runnable session = () -> {
            try { go.await(); } catch (InterruptedException e) { throw new IllegalStateException(e); }
            got.add(batches.nextBatch("30570034"));
        };
        Thread a = new Thread(session), b = new Thread(session);
        a.start(); b.start();
        go.countDown();
        a.join(10000); b.join(10000);
        sp.overlapReads = false;
        assertEquals(Set.of(301, 302), got, "two settlements, two batch numbers");
        assertEquals(302, batches.profile("30570034").batch);
    }

    /**
     * Behaves like a persistent space: every entry is serialized when written and every read
     * returns a new copy. When asked, two readers of rdp meet after reading before they go on.
     */
    static final class Copying implements Space<String, Object> {
        final TSpace<String, Object> sp = new TSpace<>();
        final CyclicBarrier readers = new CyclicBarrier(2);
        volatile boolean overlapReads;

        static byte[] freeze(Object v) {
            try (ByteArrayOutputStream b = new ByteArrayOutputStream(); ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(v);
                o.flush();
                return b.toByteArray();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        static Object thaw(Object bytes) {
            if (bytes == null) return null;
            try (ObjectInputStream i = new ObjectInputStream(new ByteArrayInputStream((byte[]) bytes))) {
                return i.readObject();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override public Object rdp(String k) {
            Object v = thaw(sp.rdp(k));
            if (overlapReads) {
                try { readers.await(1, TimeUnit.SECONDS); }
                catch (TimeoutException | BrokenBarrierException e) { /* alone: go on */ }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            return v;
        }
        @Override public void out(String k, Object v) { sp.out(k, freeze(v)); }
        @Override public void out(String k, Object v, long t) { sp.out(k, freeze(v), t); }
        @Override public void push(String k, Object v) { sp.push(k, freeze(v)); }
        @Override public void push(String k, Object v, long t) { sp.push(k, freeze(v), t); }
        @Override public void put(String k, Object v) { sp.put(k, freeze(v)); }
        @Override public void put(String k, Object v, long t) { sp.put(k, freeze(v), t); }
        @Override public Object in(String k) { return thaw(sp.in(k)); }
        @Override public Object rd(String k) { return thaw(sp.rd(k)); }
        @Override public Object in(String k, long t) { return thaw(sp.in(k, t)); }
        @Override public Object rd(String k, long t) { return thaw(sp.rd(k, t)); }
        @Override public Object inp(String k) { return thaw(sp.inp(k)); }
        @Override public void nrd(String k) { sp.nrd(k); }
        @Override public Object nrd(String k, long t) { return thaw(sp.nrd(k, t)); }
        @Override public boolean existAny(String[] ks) { return sp.existAny(ks); }
        @Override public boolean existAny(String[] ks, long t) { return sp.existAny(ks, t); }
    }
}
