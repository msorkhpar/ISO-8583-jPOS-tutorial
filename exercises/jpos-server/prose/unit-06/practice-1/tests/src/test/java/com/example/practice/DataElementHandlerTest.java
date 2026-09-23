package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionConstants;
import org.junit.jupiter.api.Test;

class DataElementHandlerTest {

    private static final int[] FIELDS = {2, 3, 4, 11, 41};

    private static ISOMsg message(String mti) throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(mti);
        msg.set(2, "4111111111111111");
        msg.set(3, "000000");
        msg.set(4, "000000012345");
        msg.set(11, "123456");
        msg.set(41, "12345678");
        return msg;
    }

    private static Context withRequest(ISOMsg msg) {
        Context ctx = new Context();
        ctx.put("REQUEST", msg);
        return ctx;
    }

    @Test
    void aRequestIsAnsweredWithAnApprovedCopyOfItself() throws Exception {
        for (String[] pair : new String[][] {{"0200", "0210"}, {"0100", "0110"}}) {
            ISOMsg request = message(pair[0]);
            Context ctx = withRequest(request);
            int result = new DataElementHandler().prepare(1L, ctx);
            assertEquals(TransactionConstants.PREPARED | TransactionConstants.NO_JOIN
                | TransactionConstants.READONLY, result);
            ISOMsg response = ctx.get("RESPONSE");
            assertEquals(pair[1], response.getMTI());
            for (int field : FIELDS) {
                assertEquals(request.getString(field), response.getString(field), "field " + field);
            }
            assertEquals("00", response.getString(39));
            assertEquals("123456", response.getString(38));
        }
    }

    @Test
    void theRequestItselfIsLeftAsItWas() throws Exception {
        ISOMsg request = message("0200");
        Context ctx = withRequest(request);
        new DataElementHandler().prepare(1L, ctx);
        assertNotSame(request, ctx.get("RESPONSE"));
        assertEquals("0200", request.getMTI());
        assertFalse(request.hasField(38));
        assertFalse(request.hasField(39));
    }

    @Test
    void aMessageThatIsAlreadyAResponseAborts() throws Exception {
        Context ctx = withRequest(message("0210"));
        int result = new DataElementHandler().prepare(1L, ctx);
        assertEquals(TransactionConstants.ABORTED | TransactionConstants.NO_JOIN
            | TransactionConstants.READONLY, result);
        assertInstanceOf(ISOException.class, ctx.get("EXCEPTION"));
        assertFalse(ctx.hasKey("RESPONSE"));
    }
}
