package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.util.LogEvent;
import org.jpos.util.LogListener;
import org.jpos.util.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EchoResponderTest {
    /** The logger the node's descriptor names; Q2 hands it to the listener. */
    private final Logger nodeLogger = new Logger();
    private final List<LogEvent> atNode = Collections.synchronizedList(new ArrayList<>());
    private final List<LogEvent> atQ2 = Collections.synchronizedList(new ArrayList<>());
    private final LogListener q2Capture = ev -> { atQ2.add(ev); return ev; };

    @BeforeEach
    void capture() {
        nodeLogger.addListener(ev -> { atNode.add(ev); return ev; });
        Logger.getLogger("Q2").addListener(q2Capture);
    }

    @AfterEach
    void release() {
        Logger.getLogger("Q2").removeListener(q2Capture);
    }

    /** The host's side of the link: records what the listener sends back, or fails. */
    static final class Link implements ISOSource {
        final List<ISOMsg> sent = new ArrayList<>();
        IOException failure;

        @Override
        public void send(ISOMsg m) throws IOException {
            if (failure != null)
                throw failure;
            sent.add(m);
        }

        @Override
        public boolean isConnected() {
            return true;
        }
    }

    private static ISOMsg echo(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0800");
        m.set(7, "1106093015");
        m.set(11, stan);
        m.set(70, "301");
        return m;
    }

    private EchoResponder responder() {
        EchoResponder r = new EchoResponder();
        r.setLogger(nodeLogger, "echo-responder");
        return r;
    }

    private List<LogEvent> everywhere() {
        List<LogEvent> all = new ArrayList<>(atNode);
        all.addAll(atQ2);
        return all;
    }

    private static String text(LogEvent ev) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ev.dump(new PrintStream(buf, true, StandardCharsets.UTF_8), "");
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void anEchoIsAnsweredAndRecorded() throws Exception {
        Link link = new Link();
        assertTrue(responder().process(link, echo("004711")), "an echo is this listener's to handle");
        assertEquals(1, link.sent.size(), "one reply goes back");
        ISOMsg reply = link.sent.get(0);
        assertEquals("0810", reply.getMTI());
        assertEquals("004711", reply.getString(11));
        assertEquals("00", reply.getString(39));
        assertTrue(everywhere().stream().anyMatch(ev -> text(ev).contains("004711")),
                "the echo is recorded somewhere in the log");
    }

    @Test
    void theEntryReachesTheLoggerQ2GaveTheListener() throws Exception {
        responder().process(new Link(), echo("004711"));
        assertFalse(atNode.isEmpty(), "the logger from the <request-listener> element gets the entry");
        for (LogEvent ev : atNode)
            assertEquals("echo-responder", ev.getRealm());
        assertTrue(atQ2.isEmpty(), "nothing lands in the Q2 logger instead: " + atQ2.size() + " event(s)");
    }

    @Test
    void eachEchoIsOneEntryWithWhatCameInAndWhatWentBack() throws Exception {
        EchoResponder r = responder();
        Link link = new Link();
        for (String stan : new String[] { "004712", "004713", "004714" })
            r.process(link, echo(stan));
        List<LogEvent> all = everywhere();
        assertEquals(3, all.size(), "one entry per echo");
        String[] stans = { "004712", "004713", "004714" };
        for (int i = 0; i < 3; i++) {
            String t = text(all.get(i));
            assertTrue(t.contains(stans[i]), "entry " + (i + 1) + " is about echo " + stans[i] + ":\n" + t);
            assertTrue(t.contains("0800"), "entry " + (i + 1) + " shows what came in:\n" + t);
            assertTrue(t.contains("0810"), "entry " + (i + 1) + " shows what went back:\n" + t);
        }
    }

    @Test
    void aReplyThatCannotGoOutIsStillRecorded() throws Exception {
        Link link = new Link();
        link.failure = new IOException("connection reset by host");
        assertTrue(responder().process(link, echo("004715")));
        List<LogEvent> all = everywhere();
        assertEquals(1, all.size(), "the echo that could not be answered has its entry");
        String t = text(all.get(0));
        assertTrue(t.contains("004715"), t);
        assertTrue(t.contains("connection reset by host"), "the entry says why nothing went back:\n" + t);
    }
}
