package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionConstants;
import org.junit.jupiter.api.Test;

class CardNetworkCheckTest {

    private static final int DONE =
        TransactionConstants.PREPARED | TransactionConstants.NO_JOIN | TransactionConstants.READONLY;

    private static ISOMsg request(String pan, int... fields) throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0100");
        if (pan != null) {
            msg.set(2, pan);
        }
        for (int field : fields) {
            msg.set(field, "DATA" + field);
        }
        return msg;
    }

    private static Context run(ISOMsg msg, int expectedResult) {
        Context ctx = new Context();
        ctx.put("REQUEST", msg);
        assertEquals(expectedResult, new CardNetworkCheck().prepare(1L, ctx));
        return ctx;
    }

    private static void noNetworkVerdict(Context ctx) {
        assertFalse(ctx.hasKey("VISA_TRANSACTION_VALID"));
        assertFalse(ctx.hasKey("MASTERCARD_TRANSACTION_VALID"));
    }

    @Test
    void eachNetworkIsCheckedAgainstItsOwnFields() throws Exception {
        Context visa = run(request("4111111111111111", 42, 43, 48), DONE);
        assertEquals("VISA", visa.get("CARD_TYPE"));
        assertEquals(Boolean.TRUE, visa.get("VISA_TRANSACTION_VALID"));
        assertNull(visa.get("VISA_ERROR"));
        assertFalse(visa.hasKey("MASTERCARD_TRANSACTION_VALID"));

        Context visaShort = run(request("4111111111111111", 42, 48, 61, 63), DONE);
        assertEquals(Boolean.FALSE, visaShort.get("VISA_TRANSACTION_VALID"));
        assertNotNull(visaShort.get("VISA_ERROR"));

        Context mc = run(request("5500000000000004", 48, 61, 63), DONE);
        assertEquals("MASTERCARD", mc.get("CARD_TYPE"));
        assertEquals(Boolean.TRUE, mc.get("MASTERCARD_TRANSACTION_VALID"));
        assertNull(mc.get("MASTERCARD_ERROR"));
        assertFalse(mc.hasKey("VISA_TRANSACTION_VALID"));

        Context mcShort = run(request("5100000000000008", 42, 43, 48, 63), DONE);
        assertEquals("MASTERCARD", mcShort.get("CARD_TYPE"));
        assertEquals(Boolean.FALSE, mcShort.get("MASTERCARD_TRANSACTION_VALID"));
        assertNotNull(mcShort.get("MASTERCARD_ERROR"));
    }

    @Test
    void onlyFiftyOneToFiftyFiveAreMastercard() throws Exception {
        for (String pan : new String[] {"5600000000000003", "5000000000000009"}) {
            Context ctx = run(request(pan, 48, 61, 63), DONE);
            assertEquals("UNKNOWN", ctx.get("CARD_TYPE"), pan);
            noNetworkVerdict(ctx);
        }
    }

    @Test
    void aRequestWithNoCardNumberIsUnknown() throws Exception {
        Context ctx = run(request(null, 42, 43, 48), DONE);
        assertEquals("UNKNOWN", ctx.get("CARD_TYPE"));
        noNetworkVerdict(ctx);
    }
}
