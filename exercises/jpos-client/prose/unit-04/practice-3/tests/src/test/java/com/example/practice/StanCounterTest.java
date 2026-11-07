package com.example.practice;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class StanCounterTest {

    @Test
    void aFreshLinkCountsFromOne() {
        StanCounter stans = new StanCounter(new StanCounter.Table(), "acquirer-b");
        assertEquals("000001", stans.next());
        assertEquals("000002", stans.next());
        assertEquals("000003", stans.next());
    }

    @Test
    void aRestartedProcessCarriesOn() {
        StanCounter.Table table = new StanCounter.Table();
        StanCounter beforeRestart = new StanCounter(table, "acquirer-b");
        for (int i = 0; i < 4; i++) beforeRestart.next();
        StanCounter afterRestart = new StanCounter(table, "acquirer-b");
        assertEquals("000005", afterRestart.next(), "the morning's numbers are not handed out again");
    }

    @Test
    void theSequenceWrapsPast999999() {
        StanCounter.Table table = new StanCounter.Table();
        table.write("acquirer-b", 999998L);
        StanCounter stans = new StanCounter(table, "acquirer-b");
        assertEquals("999999", stans.next());
        assertEquals("000001", stans.next(), "after 999999 the numbering starts again at 000001");
        assertEquals("000002", stans.next());
    }

    /** Two gateway processes read the row at the same moment, before either has written it. */
    @Test
    void twoProcessesAtTheSameMoment() throws Exception {
        CyclicBarrier bothRead = new CyclicBarrier(2);
        AtomicInteger reads = new AtomicInteger();
        StanCounter.Table table = new StanCounter.Table() {
            @Override
            public long read(String link) {
                long v = super.read(link);
                if (reads.incrementAndGet() <= 2) {
                    try {
                        bothRead.await(20, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new IllegalStateException("the two processes did not both read the row", e);
                    }
                }
                return v;
            }
        };
        table.write("acquirer-b", 41L);
        StanCounter gatewayA = new StanCounter(table, "acquirer-b");
        StanCounter gatewayB = new StanCounter(table, "acquirer-b");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(gatewayA::next);
            Future<String> b = pool.submit(gatewayB::next);
            String sa = a.get(30, TimeUnit.SECONDS), sb = b.get(30, TimeUnit.SECONDS);
            assertNotEquals(sa, sb, "two messages on one link got the same STAN");
            assertEquals(java.util.Set.of("000042", "000043"), java.util.Set.of(sa, sb));
        } finally {
            pool.shutdownNow();
        }
    }
}
