package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOChannel;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.junit.jupiter.api.Test;

class ChannelPoolTest {

    /** A channel with no wire: it counts its connects and can be told it dropped. */
    static final class FakeChannel implements ISOChannel {
        int connects;
        int disconnects;
        boolean connected;

        @Override public void connect() { connects++; connected = true; }
        @Override public void disconnect() { disconnects++; connected = false; }
        @Override public void reconnect() { disconnect(); connect(); }
        @Override public boolean isConnected() { return connected; }
        void drop() { connected = false; }
        @Override public void setPackager(ISOPackager p) { }
        @Override public ISOMsg receive() { throw new UnsupportedOperationException(); }
        @Override public void send(ISOMsg m) { }
        @Override public void send(byte[] b) { }
        @Override public void setUsable(boolean usable) { }
        @Override public void setName(String name) { }
        @Override public String getName() { return "fake"; }
        @Override public ISOPackager getPackager() { return null; }
        @Override public Object clone() { return this; }
    }

    private final List<FakeChannel> made = new ArrayList<>();

    private ChannelPool pool(int max) {
        return new ChannelPool(() -> {
            FakeChannel channel = new FakeChannel();
            made.add(channel);
            return channel;
        }, max);
    }

    @Test
    void aChannelIsConnectedOnceAndReused() throws Exception {
        ChannelPool pool = pool(2);
        List<Boolean> connectedDuringWork = new ArrayList<>();
        ISOChannel first = pool.withChannel(c -> { connectedDuringWork.add(c.isConnected()); return c; });
        assertEquals(0, pool.active());
        assertEquals(1, pool.idle());
        assertTrue(made.get(0).isConnected(), "a returned channel stays connected");
        ISOChannel second = pool.withChannel(c -> { connectedDuringWork.add(c.isConnected()); return c; });

        assertSame(first, second);
        assertEquals(List.of(true, true), connectedDuringWork);
        assertEquals(1, made.size());
        assertEquals(1, made.get(0).connects);
        assertEquals(0, made.get(0).disconnects);

        pool.close();
        assertFalse(made.get(0).isConnected(), "closing the pool disconnects its channels");
    }

    @Test
    void aChannelGoesBackEvenWhenTheWorkFails() throws Exception {
        ChannelPool pool = pool(1);
        ISOException failure = new ISOException("declined by the work");
        ISOException thrown = assertThrows(ISOException.class, () -> pool.withChannel(c -> { throw failure; }));
        assertSame(failure, thrown);
        assertEquals(0, pool.active());
        assertEquals(1, pool.idle());
        pool.close();
    }

    @Test
    void aDroppedChannelIsReplacedNotHandedOut() throws Exception {
        ChannelPool pool = pool(2);
        ISOChannel first = pool.withChannel(c -> c);
        made.get(0).drop();
        ISOChannel second = pool.withChannel(c -> { assertTrue(c.isConnected()); return c; });

        assertNotSame(first, second);
        assertEquals(2, made.size());
        assertEquals(1, made.get(0).disconnects, "the dropped channel is disconnected when thrown away");
        assertEquals(1, pool.idle());
        pool.close();
    }
}
