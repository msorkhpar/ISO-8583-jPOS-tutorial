package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaintenanceListenerTest {

    /** One terminal's connection: records what the server sends on it. */
    static final class Connection implements ISOSource {
        final List<ISOMsg> sent = new ArrayList<>();

        @Override
        public void send(ISOMsg m) {
            sent.add((ISOMsg) m.clone());
        }

        @Override
        public boolean isConnected() {
            return true;
        }
    }

    /** Stands in for IncomingListener: takes every message it is offered. */
    static final class Queue implements ISORequestListener {
        final List<ISOMsg> got = new ArrayList<>();

        @Override
        public boolean process(ISOSource source, ISOMsg m) {
            got.add(m);
            return true;
        }
    }

    /** What a QServer session does with one message: ask each listener in turn until one says true. */
    static void offer(ISOSource source, ISOMsg m, ISORequestListener... chain) {
        for (ISORequestListener l : chain)
            if (l.process(source, m))
                return;
    }

    private static ISOMsg message(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        if (mti.startsWith("08")) {
            m.set(70, "301");
        } else {
            m.set(2, "5555555555554444");
            m.set(3, "000000");
            m.set(4, "000000012345");
        }
        m.set(7, "0214221530");
        m.set(11, stan);
        m.set(41, "29110077");
        return m;
    }

    @Test
    void aPurchaseDuringMaintenanceIsDeclinedAtOnce() throws Exception {
        MaintenanceListener listener = new MaintenanceListener();
        Connection terminal = new Connection();
        listener.begin();
        offer(terminal, message("0200", "004417"), listener, new Queue());
        assertEquals(1, terminal.sent.size(), "the terminal gets one answer");
        ISOMsg answer = terminal.sent.get(0);
        assertEquals("0210", answer.getMTI());
        assertEquals("91", answer.getString(39));
        assertEquals("004417", answer.getString(11));
        assertEquals("29110077", answer.getString(41));
    }

    @Test
    void outsideMaintenanceEverythingReachesTheQueue() throws Exception {
        MaintenanceListener listener = new MaintenanceListener();
        Connection terminal = new Connection();
        Queue queue = new Queue();
        offer(terminal, message("0200", "004418"), listener, queue);
        listener.begin();
        listener.end();
        offer(terminal, message("0100", "004419"), listener, queue);
        assertTrue(terminal.sent.isEmpty(), "outside maintenance the listener answers nothing");
        assertEquals(2, queue.got.size(), "both requests reach the TransactionManager");
    }

    @Test
    void reversalsAndAdvicesReachTheQueueDuringMaintenance() throws Exception {
        MaintenanceListener listener = new MaintenanceListener();
        Connection terminal = new Connection();
        Queue queue = new Queue();
        listener.begin();
        offer(terminal, message("0420", "004420"), listener, queue);
        offer(terminal, message("0220", "004421"), listener, queue);
        assertTrue(terminal.sent.isEmpty(),
                "any answer tells the terminal its reversal or advice arrived, and it would stop sending it");
        assertEquals(2, queue.got.size(), "they are queued for the TransactionManager to store");
    }

    @Test
    void aDeclinedRequestGoesNoFurther() throws Exception {
        MaintenanceListener listener = new MaintenanceListener();
        Connection terminal = new Connection();
        Queue queue = new Queue();
        listener.begin();
        offer(terminal, message("0100", "004422"), listener, queue);
        assertEquals(1, terminal.sent.size());
        assertEquals("0110", terminal.sent.get(0).getMTI());
        assertTrue(queue.got.isEmpty(), "a declined request must not also be processed and answered again");
    }

    @Test
    void anEchoDuringMaintenanceStillReachesTheQueue() throws Exception {
        MaintenanceListener listener = new MaintenanceListener();
        Connection terminal = new Connection();
        Queue queue = new Queue();
        listener.begin();
        offer(terminal, message("0800", "004423"), listener, queue);
        assertTrue(terminal.sent.isEmpty());
        assertEquals(1, queue.got.size(), "a message the listener doesn't answer is not swallowed");
    }
}
