package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.junit.jupiter.api.Test;

class AuthorizationSenderTest {

    /** A MUX with no wire: it answers from a script, or holds the listener for the test to call. */
    static final class FakeMux implements MUX {
        final List<ISOMsg> answers = new ArrayList<>();
        final List<Long> timeouts = new ArrayList<>();
        final List<ISOMsg> sent = new ArrayList<>();
        ISOResponseListener listener;
        Object handBack;

        @Override
        public ISOMsg request(ISOMsg m, long timeout) {
            sent.add(m);
            timeouts.add(timeout);
            return answers.remove(0);
        }

        @Override
        public void request(ISOMsg m, long timeout, ISOResponseListener rl, Object handBack) {
            sent.add(m);
            timeouts.add(timeout);
            this.listener = rl;
            this.handBack = handBack;
        }

        @Override public void send(ISOMsg m) { sent.add(m); }
        @Override public boolean isConnected() { return true; }
    }

    private static ISOMsg request() throws ISOException {
        ISOMsg m = new ISOMsg();
        m.setMTI("0100");
        m.set(11, "000042");
        return m;
    }

    private static ISOMsg response(String code) throws ISOException {
        ISOMsg m = new ISOMsg();
        m.setMTI("0110");
        m.set(11, "000042");
        m.set(39, code);
        return m;
    }

    @Test
    void aWaitedAnswerIsReadFromFieldThirtyNine() throws Exception {
        FakeMux mux = new FakeMux();
        mux.answers.add(response("00"));
        mux.answers.add(response("05"));
        AuthorizationSender sender = new AuthorizationSender(mux, 1500);
        ISOMsg req = request();

        assertEquals(AuthorizationSender.Outcome.APPROVED, sender.authorize(req));
        assertEquals(AuthorizationSender.Outcome.DECLINED, sender.authorize(request()));
        assertSame(req, mux.sent.get(0));
        assertEquals(List.of(1500L, 1500L), mux.timeouts);
    }

    @Test
    void anAsynchronousAnswerCompletesTheFuture() throws Exception {
        FakeMux mux = new FakeMux();
        AuthorizationSender sender = new AuthorizationSender(mux, 2500);
        ISOMsg req = request();

        CompletableFuture<AuthorizationSender.Outcome> outcome = sender.authorizeAsync(req);
        assertFalse(outcome.isDone(), "the future is returned before any answer");
        assertSame(req, mux.sent.get(0));
        assertEquals(List.of(2500L), mux.timeouts);
        assertNotNull(mux.listener);

        mux.listener.responseReceived(response("00"), mux.handBack);
        assertTrue(outcome.isDone());
        assertEquals(AuthorizationSender.Outcome.APPROVED, outcome.getNow(null));
    }

    @Test
    void noAnswerInTimeIsNoAnswerNotAnError() throws Exception {
        FakeMux mux = new FakeMux();
        mux.answers.add(null);
        AuthorizationSender sender = new AuthorizationSender(mux, 1500);
        assertEquals(AuthorizationSender.Outcome.NO_ANSWER, sender.authorize(request()));
    }

    @Test
    void anExpiredRequestCompletesAsNoAnswer() throws Exception {
        FakeMux mux = new FakeMux();
        AuthorizationSender sender = new AuthorizationSender(mux, 2500);
        CompletableFuture<AuthorizationSender.Outcome> outcome = sender.authorizeAsync(request());

        mux.listener.expired(mux.handBack);
        assertTrue(outcome.isDone(), "an expired request must not leave its caller waiting");
        assertEquals(AuthorizationSender.Outcome.NO_ANSWER, outcome.getNow(null));
    }
}
