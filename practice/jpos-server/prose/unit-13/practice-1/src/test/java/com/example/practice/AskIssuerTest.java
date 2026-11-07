package com.example.practice;

import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionConstants;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.jpos.transaction.ContextConstants.REQUEST;
import static org.jpos.transaction.ContextConstants.TIMESTAMP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Calls AskIssuer the way the TransactionManager does, with an issuer link that records each request. */
class AskIssuerTest {
    /** The issuer's link: approves every request at once and remembers the timeout it was given. */
    static final class IssuerLink implements MUX {
        final List<ISOMsg> sent = new ArrayList<>();
        final List<Long> timeouts = new ArrayList<>();

        @Override
        public synchronized ISOMsg request(ISOMsg m, long timeout) {
            sent.add(m);
            timeouts.add(timeout);
            try {
                ISOMsg r = (ISOMsg) m.clone();
                r.setResponseMTI();
                r.set(39, "00");
                return r;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void request(ISOMsg m, long timeout, ISOResponseListener l, Object handBack) {
            throw new UnsupportedOperationException("not used here");
        }

        @Override
        public void send(ISOMsg m) {
            throw new UnsupportedOperationException("not used here");
        }

        @Override
        public boolean isConnected() {
            return true;
        }
    }

    private IssuerLink link;
    private AskIssuer participant;

    @BeforeEach
    void setUp() throws Exception {
        link = new IssuerLink();
        NameRegistrar.register("mux.atm-issuer", link);
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("mux", "atm-issuer");
        cfg.put("terminal-wait", "15000");
        participant = new AskIssuer();
        participant.setConfiguration(cfg);
    }

    @AfterEach
    void tearDown() {
        NameRegistrar.unregister("mux.atm-issuer");
    }

    /** A withdrawal from ATM00417 that arrived ageMs ago, as IncomingListener would have stored it. */
    static Context withdrawal(String stan, long ageMs) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "010000");
        m.set(4, "000000012000");
        m.set(11, stan);
        m.set(41, "ATM00417");
        Context ctx = new Context();
        ctx.put(REQUEST.toString(), m);
        ctx.put(TIMESTAMP.toString(), new Date(System.currentTimeMillis() - ageMs));
        return ctx;
    }

    static boolean aborted(int vote) {
        return (vote & TransactionConstants.PREPARED) == 0;
    }

    @Test
    void aFreshWithdrawalGetsTheIssuersAnswer() throws Exception {
        Context ctx = withdrawal("004101", 0);
        int vote = participant.prepare(1, ctx);
        assertFalse(aborted(vote), "a withdrawal with time left goes on");
        ISOMsg answer = ctx.get(AskIssuer.ISSUER_RESPONSE);
        assertNotNull(answer, "the issuer's answer is left for the next participant");
        assertEquals("004101", answer.getString(11));
        assertEquals(1, link.sent.size());
    }

    @Test
    void theIssuerGetsOnlyWhatTheAtmStillWaits() throws Exception {
        Context ctx = withdrawal("004102", 6000);
        participant.prepare(2, ctx);
        assertEquals(1, link.sent.size());
        long given = link.timeouts.get(0);
        assertTrue(given <= 9000 && given > 8000,
            "6 s after arrival, with an ATM that waits 15 s, the issuer may have about 9 s; it got " + given);
    }

    @Test
    void aWithdrawalTheAtmGaveUpOnIsNotSent() throws Exception {
        Context ctx = withdrawal("004103", 16500);
        int vote = participant.prepare(3, ctx);
        assertEquals(0, link.sent.size(), "nothing goes to the issuer once the ATM has stopped waiting");
        assertTrue(aborted(vote), "the transaction stops");
        assertNull(ctx.get(AskIssuer.ISSUER_RESPONSE));
        assertNotNull(ctx.getResult().failure(), "the Result says why, for the participant that answers");
    }
}
