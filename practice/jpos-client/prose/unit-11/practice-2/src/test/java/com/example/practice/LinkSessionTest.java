package com.example.practice;

import org.jpos.space.TSpace;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;

import static com.example.practice.LinkSession.Action.LOGON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LinkSessionTest {
    private static final String MUX = "acq-mux";
    private static final String READY = "acq-channel.ready";
    private static final long HOUR = 3_600_000L;

    private final TSpace<String, Object> sp = new TSpace<>();

    /** Plays the host: records each code it receives and answers from a script (yes when it runs out). */
    final class ScriptedHost implements LinkSession.Exchanger {
        final List<String> codes = new ArrayList<>();
        final List<Object> enabledWhenSent = new ArrayList<>();
        final Deque<Boolean> answers = new ArrayDeque<>();

        @Override
        public boolean exchange(String muxName, String code) {
            codes.add(code);
            enabledWhenSent.add(sp.rdp(MUX + ".enabled"));
            return answers.isEmpty() || answers.poll();
        }
    }

    private LinkSession session(ScriptedHost host) {
        return new LinkSession(sp, MUX, READY, host, HOUR, 24 * HOUR);
    }

    @Test
    void aSignedOnLinkIsSignedOffAndLeavesService() {
        ScriptedHost host = new ScriptedHost();
        LinkSession s = session(host);
        sp.out(READY, new Date(1_762_380_000_000L));
        s.step();
        s.signOff();
        assertEquals(List.of("001", "002"), host.codes);
        assertNull(sp.rdp(MUX + ".enabled"), "the link is out of service after the sign-off");
    }

    @Test
    void noPurchaseCanReachTheLinkWhileItSignsOff() {
        ScriptedHost host = new ScriptedHost();
        LinkSession s = session(host);
        sp.out(READY, new Date(1_762_380_000_000L));
        s.step();
        s.signOff();
        assertEquals("002", host.codes.get(1));
        assertNull(host.enabledWhenSent.get(1), "the link was still in service when 002 went out");
    }

    @Test
    void aConnectionThatNeverSignedOnGetsNoSignOff() {
        ScriptedHost host = new ScriptedHost();
        long t = 1_762_380_000_456L;
        sp.out(MUX + ".logon", new Date(t), HOUR);     // signed on a connection that is gone
        sp.out(READY, new Date(t));                    // a new connection, same millisecond
        session(host).signOff();
        assertEquals(List.of(), host.codes);
    }

    @Test
    void aLinkThatIsDownGetsNoSignOff() {
        ScriptedHost host = new ScriptedHost();
        sp.out(MUX + ".logon", new Date(1_762_380_000_000L), HOUR);
        session(host).signOff();
        assertEquals(List.of(), host.codes);
    }

    @Test
    void deployedAgainOnTheSameConnectionTheLinkComesBack() {
        ScriptedHost host = new ScriptedHost();
        Date connection = new Date(1_762_380_000_000L);
        sp.out(READY, connection);
        session(host).step();
        session(host).signOff();

        LinkSession again = session(host);
        assertEquals(LOGON, again.step());
        assertSame(connection, sp.rdp(MUX + ".enabled"));
    }

    @Test
    void aSignOffTheHostNeverAnsweredStillEndsTheSession() {
        ScriptedHost host = new ScriptedHost();
        Date connection = new Date(1_762_380_000_000L);
        sp.out(READY, connection);
        session(host).step();
        host.answers.add(false);                       // the 002 gets no answer
        session(host).signOff();

        assertEquals(LOGON, session(host).step());
        assertSame(connection, sp.rdp(MUX + ".enabled"));
    }
}
