package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalDirectoryTest {

    /** One accepted connection, as a QServer session sees it. */
    static final class Connection implements ISOSource {
        final List<ISOMsg> sent = new ArrayList<>();
        boolean connected = true;

        @Override
        public void send(ISOMsg m) throws IOException {
            if (!connected)
                throw new IOException("unconnected ISOChannel");
            sent.add((ISOMsg) m.clone());
        }

        @Override
        public boolean isConnected() {
            return connected;
        }
    }

    /** Stands in for the listeners after the directory: takes every message it is offered. */
    static final class Rest implements ISORequestListener {
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

    private static ISOMsg fromTerminal(String mti, String terminal, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(7, "0309081200");
        m.set(11, stan);
        if (terminal != null)
            m.set(41, terminal);
        m.set(70, "301");
        return m;
    }

    private static ISOMsg signOff(String terminal) throws Exception {
        ISOMsg m = new ISOMsg("0800");
        m.set(7, "0309081500");
        m.set(11, "880014");
        m.set(41, terminal);
        m.set(70, "002");
        return m;
    }

    @Test
    void aTerminalGetsAMessageAddressedToIt() throws Exception {
        TerminalDirectory dir = new TerminalDirectory();
        Connection c = new Connection();
        offer(c, fromTerminal("0800", "29110041", "000913"), dir, new Rest());
        assertTrue(dir.send(signOff("29110041")));
        assertEquals(1, c.sent.size());
        assertEquals("002", c.sent.get(0).getString(70));
    }

    @Test
    void everyMessageGoesOnToTheRestOfTheChain() throws Exception {
        TerminalDirectory dir = new TerminalDirectory();
        Rest rest = new Rest();
        Connection c = new Connection();
        offer(c, fromTerminal("0800", "29110041", "000914"), dir, rest);
        offer(c, fromTerminal("0200", "29110041", "000915"), dir, rest);
        assertEquals(2, rest.got.size(), "the directory only watches; the requests still need their answers");
    }

    @Test
    void aTerminalThatReconnectedIsReachedOnItsNewConnection() throws Exception {
        TerminalDirectory dir = new TerminalDirectory();
        Connection before = new Connection(), after = new Connection();
        offer(before, fromTerminal("0800", "29110041", "000916"), dir, new Rest());
        before.connected = false;
        offer(after, fromTerminal("0800", "29110041", "000917"), dir, new Rest());
        assertTrue(dir.send(signOff("29110041")));
        assertEquals(1, after.sent.size(), "the terminal is on its new connection now");
        assertTrue(before.sent.isEmpty());
    }

    @Test
    void aTerminalThatIsGoneIsReportedNotReached() throws Exception {
        TerminalDirectory dir = new TerminalDirectory();
        Connection c = new Connection();
        offer(c, fromTerminal("0800", "29110041", "000918"), dir, new Rest());
        c.connected = false;
        assertFalse(dir.send(signOff("29110041")), "its only connection is closed");
        assertFalse(dir.send(signOff("29110099")), "this terminal never connected");
        assertTrue(c.sent.isEmpty());
    }

    @Test
    void eachTerminalGetsOnlyWhatIsAddressedToIt() throws Exception {
        TerminalDirectory dir = new TerminalDirectory();
        Connection first = new Connection(), second = new Connection();
        offer(first, fromTerminal("0800", "29110041", "000919"), dir, new Rest());
        offer(second, fromTerminal("0800", "29110042", "000301"), dir, new Rest());
        assertTrue(dir.send(signOff("29110041")));
        assertEquals(1, first.sent.size());
        assertTrue(second.sent.isEmpty(), "the terminal that connected last gets nothing meant for another");
    }

    @Test
    void aMessageWithoutATerminalIdStillGoesOn() throws Exception {
        TerminalDirectory dir = new TerminalDirectory();
        Rest rest = new Rest();
        offer(new Connection(), fromTerminal("0800", null, "000920"), dir, rest);
        assertEquals(1, rest.got.size(), "not every message carries field 41");
    }
}
