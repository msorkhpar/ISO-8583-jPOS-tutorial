package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CardCheckListenerTest {
    private static final long WAIT = 10;                   // seconds; generous, never reached when right

    /** One terminal's connection: what the server sends on it, in order. */
    static final class Connection implements ISOSource {
        final BlockingQueue<ISOMsg> sent = new LinkedBlockingQueue<>();

        @Override
        public void send(ISOMsg m) {
            sent.add((ISOMsg) m.clone());
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        ISOMsg next() throws InterruptedException {
            return sent.poll(WAIT, TimeUnit.SECONDS);
        }
    }

    /** A card table whose lookups for chosen cards wait until the test lets them finish. */
    static final class Table implements CardCheckListener.CardTable {
        final Map<String, CountDownLatch> held = new ConcurrentHashMap<>();

        CountDownLatch hold(String pan) {
            CountDownLatch gate = new CountDownLatch(1);
            held.put(pan, gate);
            return gate;
        }

        @Override
        public boolean isBlocked(String pan) throws Exception {
            CountDownLatch gate = held.get(pan);
            if (gate != null && !gate.await(60, TimeUnit.SECONDS))
                throw new IllegalStateException("lookup never released");
            return pan.startsWith("5555");
        }
    }

    private static ISOMsg purchase(String pan, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, "000000004990");
        m.set(11, stan);
        m.set(41, "29110063");
        return m;
    }

    /** Calls process the way a session thread does, and fails if it doesn't return in time. */
    private static boolean offer(CardCheckListener l, ISOSource s, ISOMsg m) throws Exception {
        ExecutorService session = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> r = session.submit(() -> l.process(s, m));
            return r.get(WAIT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("process() was still running after " + WAIT + " s; the connection reads nothing meanwhile");
            return false;
        } finally {
            session.shutdown();
        }
    }

    @Test
    void everyPurchaseIsAnsweredOnItsConnection() throws Exception {
        CardCheckListener l = new CardCheckListener(new Table());
        Connection c = new Connection();
        assertTrue(offer(l, c, purchase("4111111111111111", "007301")));
        assertTrue(offer(l, c, purchase("5555555555554444", "007302")));
        ISOMsg a = c.next(), b = c.next();
        assertNotNull(a, "an answer arrives");
        assertNotNull(b, "an answer arrives");
        ISOMsg ok = "007301".equals(a.getString(11)) ? a : b;
        ISOMsg blocked = ok == a ? b : a;
        assertEquals("0210", ok.getMTI());
        assertEquals("00", ok.getString(39));
        assertEquals("007302", blocked.getString(11));
        assertEquals("62", blocked.getString(39));
    }

    @Test
    void theConnectionIsFreeWhileTheLookupRuns() throws Exception {
        Table t = new Table();
        CountDownLatch gate = t.hold("4111111111111111");
        try {
            CardCheckListener l = new CardCheckListener(t);
            Connection c = new Connection();
            assertTrue(offer(l, c, purchase("4111111111111111", "007311")));
            assertNull(c.sent.peek(), "the lookup is still running, so there is no answer yet");
            gate.countDown();
            assertEquals("007311", c.next().getString(11));
        } finally {
            gate.countDown();
        }
    }

    @Test
    void aQuickLookupIsNotStuckBehindASlowOne() throws Exception {
        Table t = new Table();
        CountDownLatch slow = t.hold("4111111111111111");
        try {
            CardCheckListener l = new CardCheckListener(t);
            Connection c = new Connection();
            offer(l, c, purchase("4111111111111111", "007321"));
            offer(l, c, purchase("5555555555554444", "007322"));
            ISOMsg first = c.next();
            assertNotNull(first, "the second purchase was answered while the first lookup was still running");
            assertEquals("007322", first.getString(11));
        } finally {
            slow.countDown();
        }
    }
}
