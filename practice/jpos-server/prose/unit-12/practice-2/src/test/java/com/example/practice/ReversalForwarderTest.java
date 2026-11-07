package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.space.TSpace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReversalForwarderTest {
    static final String QUEUE = "reversal.visa";

    static ISOMsg reversal(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0420");
        m.set(4, "000000003150");
        m.set(11, stan);
        m.set(41, "30570021");
        return m;
    }

    /** What the host received, in order, and a latch that counts arrivals. */
    static final class Host implements Consumer<ISOMsg> {
        final List<String> stans = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch arrived;
        final CountDownLatch answers;                    // the host answers once this opens

        Host(int expected, CountDownLatch answers) {
            this.arrived = new CountDownLatch(expected);
            this.answers = answers;
        }

        @Override
        public void accept(ISOMsg m) {
            try {
                answers.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            stans.add(m == null ? null : m.getString(11));
            arrived.countDown();
        }

        void awaitAll() throws InterruptedException {
            assertTrue(arrived.await(20, TimeUnit.SECONDS), "every reversal reaches the host");
        }
    }

    static CountDownLatch open() {
        return new CountDownLatch(0);
    }

    @Test
    void everyQueuedReversalReachesTheSender() throws Exception {
        TSpace<String, Object> sp = new TSpace<>();
        Host host = new Host(3, open());
        ReversalForwarder f = new ReversalForwarder(sp, QUEUE, host);
        for (String stan : new String[] { "000811", "000812", "000813" })
            sp.out(QUEUE, reversal(stan));
        host.awaitAll();
        f.close();
        assertEquals(new HashSet<>(List.of("000811", "000812", "000813")), new HashSet<>(host.stans));
    }

    @Test
    void theParticipantThatQueuesAReversalDoesNotWaitForTheHost() throws Exception {
        TSpace<String, Object> sp = new TSpace<>();
        CountDownLatch hostAnswers = new CountDownLatch(1);
        Host host = new Host(1, hostAnswers);
        ReversalForwarder f = new ReversalForwarder(sp, QUEUE, host);
        Thread participant = new Thread(() -> {
            try { sp.out(QUEUE, reversal("000821")); } catch (Exception e) { throw new IllegalStateException(e); }
        });
        participant.start();
        participant.join(5000);
        boolean returned = !participant.isAlive();
        hostAnswers.countDown();
        host.awaitAll();
        f.close();
        assertTrue(returned, "queuing a reversal must not wait until the host has answered it");
    }

    @Test
    void aForwardedReversalLeavesTheQueue() throws Exception {
        TSpace<String, Object> sp = new TSpace<>();
        Host host = new Host(2, open());
        ReversalForwarder f = new ReversalForwarder(sp, QUEUE, host);
        sp.out(QUEUE, reversal("000831"));
        sp.out(QUEUE, reversal("000832"));
        host.awaitAll();
        f.close();
        assertNull(sp.rdp(QUEUE), "a reversal handed to the sender is no longer queued");
    }

    @Test
    void twoForwardersOnOneQueueSendEachReversalOnce() throws Exception {
        TSpace<String, Object> sp = new TSpace<>();
        Host host = new Host(4, open());
        ReversalForwarder first = new ReversalForwarder(sp, QUEUE, host);
        ReversalForwarder second = new ReversalForwarder(sp, QUEUE, host);
        for (String stan : new String[] { "000841", "000842", "000843", "000844" })
            sp.out(QUEUE, reversal(stan));
        host.awaitAll();
        first.close();
        second.close();
        List<String> sent = new ArrayList<>(host.stans);
        Collections.sort(sent, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
        assertEquals(List.of("000841", "000842", "000843", "000844"), sent,
            "each reversal goes to the host exactly once");
    }
}
