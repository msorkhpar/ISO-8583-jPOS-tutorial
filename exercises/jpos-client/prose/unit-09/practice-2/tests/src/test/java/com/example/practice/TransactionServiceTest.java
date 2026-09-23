package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.TimeoutException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.junit.jupiter.api.Test;

class TransactionServiceTest {

    /** A MUX that answers every request with one prepared reply, or with none. */
    private static final class OneReply implements MUX {
        private final ISOMsg reply;
        long timeoutAskedFor = -1;

        OneReply(ISOMsg reply) {
            this.reply = reply;
        }

        @Override
        public ISOMsg request(ISOMsg m, long timeout) {
            timeoutAskedFor = timeout;
            return reply;
        }

        @Override
        public void request(ISOMsg m, long timeout, ISOResponseListener r, Object handBack) {
            throw new AssertionError("the asynchronous request is not needed here");
        }

        @Override
        public void send(ISOMsg m) {
            throw new AssertionError("send expects no reply; use request");
        }

        @Override
        public boolean isConnected() {
            return true;
        }
    }

    private static ISOMsg request() throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        msg.set(4, "000000010000");
        msg.set(11, "000123");
        return msg;
    }

    private static ISOMsg reply(String code) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0210");
        msg.set(11, "000123");
        if (code != null) {
            msg.set(39, code);
        }
        return msg;
    }

    @Test
    void anApprovedReplyIsReturned() throws Exception {
        ISOMsg approved = reply("00");
        OneReply mux = new OneReply(approved);
        assertSame(approved, new TransactionService(mux, 30000).send(request()));
        assertEquals(30000, mux.timeoutAskedFor);
    }

    @Test
    void aDeclineRaisesAnExceptionCarryingItsCode() throws Exception {
        TransactionService service = new TransactionService(new OneReply(reply("51")), 30000);
        TransactionService.TransactionDeclinedException declined = assertThrows(
                TransactionService.TransactionDeclinedException.class, () -> service.send(request()));
        assertEquals("51", declined.getResponseCode());
    }

    @Test
    void noReplyIsATimeoutNotADecline() {
        TransactionService service = new TransactionService(new OneReply(null), 30000);
        assertThrows(TimeoutException.class, () -> service.send(request()));
    }

    @Test
    void aReplyWithoutAResponseCodeIsNotAnApproval() throws Exception {
        TransactionService service = new TransactionService(new OneReply(reply(null)), 30000);
        TransactionService.TransactionDeclinedException declined = assertThrows(
                TransactionService.TransactionDeclinedException.class, () -> service.send(request()));
        assertNull(declined.getResponseCode());
    }
}
