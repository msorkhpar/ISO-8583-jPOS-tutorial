package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.junit.jupiter.api.Test;

class ApprovingListenerTest {

    /** Stands in for the connection a request arrived on. */
    private static final class Source implements ISOSource {
        final List<ISOMsg> sent = new ArrayList<>();
        final boolean broken;

        Source(boolean broken) {
            this.broken = broken;
        }

        @Override
        public void send(ISOMsg m) throws IOException {
            if (broken) {
                throw new IOException("connection reset");
            }
            sent.add(m);
        }

        @Override
        public boolean isConnected() {
            return !broken;
        }
    }

    private static ISOMsg request(String mti) throws Exception {
        ISOMsg request = new ISOMsg();
        request.setMTI(mti);
        request.set(3, "000000");
        request.set(4, "000000010000");
        request.set(11, "000123");
        request.set(41, "12345678");
        return request;
    }

    @Test
    void answersARequestWithItsResponseThroughItsSource() throws Exception {
        Source source = new Source(false);
        assertTrue(new ApprovingListener().process(source, request("0200")));
        assertEquals(1, source.sent.size());
        ISOMsg reply = source.sent.get(0);
        assertEquals("0210", reply.getMTI());
        assertEquals("00", reply.getString(39));
        assertEquals("000000010000", reply.getString(4));
        assertEquals("000123", reply.getString(11));
        assertEquals("12345678", reply.getString(41));

        Source echo = new Source(false);
        ISOMsg networkCheck = new ISOMsg();
        networkCheck.setMTI("0800");
        networkCheck.set(70, "301");
        assertTrue(new ApprovingListener().process(echo, networkCheck));
        assertEquals("0810", echo.sent.get(0).getMTI());
        assertEquals("301", echo.sent.get(0).getString(70));
    }

    @Test
    void theRequestItselfIsLeftUnchanged() throws Exception {
        ISOMsg request = request("0200");
        new ApprovingListener().process(new Source(false), request);
        assertEquals("0200", request.getMTI());
        assertFalse(request.hasField(39));
    }

    @Test
    void aReplyThatCannotBeSentIsNotClaimed() throws Exception {
        assertFalse(new ApprovingListener().process(new Source(true), request("0200")));
    }
}
