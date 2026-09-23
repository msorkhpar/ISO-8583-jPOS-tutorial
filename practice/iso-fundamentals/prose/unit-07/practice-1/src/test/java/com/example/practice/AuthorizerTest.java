package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.junit.jupiter.api.Test;

class AuthorizerTest {

    /** A MUX that answers every request with one canned response, and remembers what it was sent. */
    private static final class CannedMux implements MUX {
        private final ISOMsg answer;
        ISOMsg sent;
        long waited = -1;

        CannedMux(ISOMsg answer) {
            this.answer = answer;
        }

        @Override
        public ISOMsg request(ISOMsg m, long timeout) {
            sent = m;
            waited = timeout;
            return answer;
        }

        @Override
        public void request(ISOMsg m, long timeout, ISOResponseListener r, Object handBack) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void send(ISOMsg m) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public boolean isConnected() {
            return true;
        }
    }

    private static ISOMsg request() throws Exception {
        ISOMsg request = new ISOMsg();
        request.setMTI("0100");
        request.set(2, "4111111111111111");
        request.set(4, "100000");
        return request;
    }

    private static ISOMsg answer(String code) throws Exception {
        ISOMsg response = new ISOMsg();
        response.setMTI("0110");
        response.set(39, code);
        return response;
    }

    @Test
    void approvesOn00AndPassesADeclineCodeOn() throws Exception {
        ISOMsg request = request();
        CannedMux approving = new CannedMux(answer("00"));
        Authorizer.Decision approved = Authorizer.authorize(approving, request);
        assertTrue(approved.approved());
        assertEquals("00", approved.responseCode());
        assertSame(request, approving.sent);
        assertEquals(Authorizer.TIMEOUT_MILLIS, approving.waited);

        Authorizer.Decision declined = Authorizer.authorize(new CannedMux(answer("51")), request());
        assertFalse(declined.approved());
        assertEquals("51", declined.responseCode());
    }

    @Test
    void noAnswerIsAnErrorNotADecline() throws Exception {
        ISOMsg request = request();
        assertThrows(java.io.IOException.class, () -> Authorizer.authorize(new CannedMux(null), request));
    }
}
