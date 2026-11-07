package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.space.Space;
import org.jpos.space.TSpace;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HostLogTest {
    private static final String QUEUE = "host.received";

    /** A space that tells the test when the code under test first looks at it. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final class WatchedSpace extends TSpace {
        final CountDownLatch looked = new CountDownLatch(1);

        @Override public synchronized Object in(Object key) { looked.countDown(); return super.in(key); }
        @Override public synchronized Object in(Object key, long timeout) { looked.countDown(); return super.in(key, timeout); }
        @Override public synchronized Object rd(Object key) { looked.countDown(); return super.rd(key); }
        @Override public synchronized Object rd(Object key, long timeout) { looked.countDown(); return super.rd(key, timeout); }
        @Override public synchronized Object inp(Object key) { Object v = super.inp(key); looked.countDown(); return v; }
        @Override public synchronized Object rdp(Object key) { Object v = super.rdp(key); looked.countDown(); return v; }
    }

    @SuppressWarnings("unchecked")
    private static Space<String, Object> typed(WatchedSpace sp) {
        return (Space<String, Object>) (Space<?, ?>) sp;
    }

    private static ISOMsg message(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(11, stan);
        m.set(41, "29110063");
        return m;
    }

    @Test
    void aMessageAlreadyReceivedIsReturned() throws Exception {
        WatchedSpace sp = new WatchedSpace();
        sp.out(QUEUE, message("0420", "004711"));
        ISOMsg got = new HostLog(typed(sp), QUEUE).awaitReceived("0420", 5000);
        assertEquals("004711", got.getString(11));
    }

    @Test
    void aMessageThatArrivesAfterTheWaitBeganIsReturned() throws Exception {
        WatchedSpace sp = new WatchedSpace();
        Thread host = new Thread(() -> {
            try {
                if (sp.looked.await(20, TimeUnit.SECONDS))
                    sp.out(QUEUE, message("0420", "004722"));    // only once the test is looking
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        host.start();
        ISOMsg got = new HostLog(typed(sp), QUEUE).awaitReceived("0420", 10000);
        assertEquals("004722", got.getString(11));
        host.join();
    }

    @Test
    void anEchoThatArrivesFirstIsNotTheReversal() throws Exception {
        WatchedSpace sp = new WatchedSpace();
        sp.out(QUEUE, message("0800", "004730"));
        sp.out(QUEUE, message("0420", "004731"));
        ISOMsg got = new HostLog(typed(sp), QUEUE).awaitReceived("0420", 5000);
        assertEquals("0420", got.getMTI());
        assertEquals("004731", got.getString(11));
    }

    @Test
    void nothingArrivingFailsWithATimeout() throws Exception {
        WatchedSpace sp = new WatchedSpace();
        sp.out(QUEUE, message("0800", "004740"));
        assertThrows(TimeoutException.class, () -> new HostLog(typed(sp), QUEUE).awaitReceived("0420", 300));
    }
}
