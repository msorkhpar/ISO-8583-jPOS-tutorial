package com.example.practice;

import com.example.practice.EchoProbe.Result;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.ASCIIChannel;
import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EchoProbeTest {

    private static ISOMsg msg(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(11, stan);
        m.set(70, "301");
        if (mti.endsWith("10")) m.set(39, "00");
        return m;
    }

    /**
     * A channel with no socket. Each receive() plays the next step of the host's script, and the
     * clock moves to the moment that step happens. Reading past the script fails the test.
     */
    static final class ScriptedChannel extends ASCIIChannel {
        final AtomicLong now = new AtomicLong(10_000);
        final List<ISOMsg> sent = new ArrayList<>();
        private final Deque<Object[]> script = new ArrayDeque<>();
        private boolean connected = true;

        ScriptedChannel frame(long atMillis, ISOMsg m) { script.add(new Object[] { atMillis, m }); return this; }
        ScriptedChannel silence(long readTimeoutMillis) { script.add(new Object[] { readTimeoutMillis, "silence" }); return this; }
        ScriptedChannel hangUp(long atMillis) { script.add(new Object[] { atMillis, "hang-up" }); return this; }

        @Override public boolean isConnected() { return connected; }

        @Override public void send(ISOMsg m) throws IOException {
            if (!connected) throw new IOException("unconnected ISOChannel");
            sent.add(m);
        }

        @Override public ISOMsg receive() throws IOException {
            if (!connected) throw new IOException("unconnected ISOChannel");
            Object[] step = script.poll();
            if (step == null)
                throw new AssertionError("the probe read on after the host's script ended, at " + now.get() + " ms");
            long t = (Long) step[0];
            if ("silence".equals(step[1])) {
                now.addAndGet(t);
                connected = false;                      // a 2.1.7 channel closes its socket on a read timeout
                throw new SocketTimeoutException("Read timed out");
            }
            now.set(Math.max(now.get(), 10_000 + t));
            if ("hang-up".equals(step[1])) {
                connected = false;
                throw new EOFException();
            }
            return (ISOMsg) step[1];
        }
    }

    private static Result probe(ScriptedChannel ch, String stan, long budget) throws Exception {
        return new EchoProbe(ch, ch.now::get).check(stan, budget);
    }

    @Test
    void anAnsweredEchoMeansTheLinkIsUp() throws Exception {
        ScriptedChannel ch = new ScriptedChannel().frame(40, msg("0810", "031415"));
        assertEquals(Result.UP, probe(ch, "031415", 5000));
        assertEquals(1, ch.sent.size(), "one echo went out");
        assertEquals("0800", ch.sent.get(0).getMTI());
        assertEquals("031415", ch.sent.get(0).getString(11));
    }

    @Test
    void theHostsOwnEchoIsNotTheAnswer() throws Exception {
        ScriptedChannel ch = new ScriptedChannel()
                .frame(30, msg("0800", "031416"))       // the host numbers its own messages; today it is at our STAN
                .silence(3000);
        assertEquals(Result.NO_ANSWER, probe(ch, "031416", 5000));
    }

    @Test
    void anAnswerToAnEarlierEchoIsNotTheAnswer() throws Exception {
        ScriptedChannel ch = new ScriptedChannel()
                .frame(30, msg("0810", "031399"))       // the late answer to a probe that already gave up
                .silence(3000);
        assertEquals(Result.NO_ANSWER, probe(ch, "031417", 5000));
    }

    @Test
    void aBusyHostDoesNotStretchTheBudget() throws Exception {
        ScriptedChannel ch = new ScriptedChannel()      // the host talks every 900 ms and never answers
                .frame(900, msg("0800", "550002"))
                .frame(1800, msg("0800", "550003"))
                .frame(2700, msg("0800", "550004"))
                .frame(3600, msg("0800", "550005"))
                .frame(4500, msg("0800", "550006"));
        assertEquals(Result.NO_ANSWER, probe(ch, "031418", 2500));
    }

    @Test
    void silenceMeansNoAnswer() throws Exception {
        ScriptedChannel ch = new ScriptedChannel().silence(3000);
        assertEquals(Result.NO_ANSWER, probe(ch, "031419", 5000));
    }

    @Test
    void aHostThatHangsUpHasTakenTheLinkDown() throws Exception {
        ScriptedChannel ch = new ScriptedChannel().hangUp(15);
        assertEquals(Result.LINK_DOWN, probe(ch, "031420", 5000));
    }
}
