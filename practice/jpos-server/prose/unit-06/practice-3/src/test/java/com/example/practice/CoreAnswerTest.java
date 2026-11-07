package com.example.practice;

import org.jpos.iso.ISOChannel;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class CoreAnswerTest {
    private static final String EIGHTY_SEVEN = "jar:packager/iso87ascii.xml";
    private static final String NINETY_THREE = "jar:packager/iso93ascii.xml";

    private static ISOMsg purchase() throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, "000000003150");
        m.set(7, "0819134030");
        m.set(11, "000277");
        m.set(41, "29110005");
        m.set(49, "978");
        return m;
    }

    private record Outcome(int vote, ISOMsg response, ISOChannel channel) {
        String rc() { return response.getString(39); }
    }

    /** Runs the participant for a request that came in on a channel with that layout. */
    private static Outcome run(String layout, CoreAnswer.Decision decision) throws Exception {
        ISOChannel channel = new ASCIIChannel(new GenericPackager(layout));
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), purchase());
        ctx.put(ContextConstants.SOURCE.toString(), channel);
        ctx.put(CoreAnswer.CORE, decision);
        int vote = new CoreAnswer().prepare(11L, ctx);
        ISOMsg response = ctx.get(ContextConstants.RESPONSE.toString());
        assertNotNull(response, "the response is in the Context");
        assertEquals("000277", response.getString(11));
        ISOMsg wire = (ISOMsg) response.clone();                // what SendResponse's channel will do
        wire.setPackager(channel.getPackager());
        try {
            wire.pack();
        } catch (Exception e) {
            fail("SendResponse could not send this response: " + e.getMessage());
        }
        return new Outcome(vote, response, channel);
    }

    @Test
    void anApprovalCarriesTheCoresCodeAndMessage() throws Exception {
        Outcome o = run(EIGHTY_SEVEN, new CoreAnswer.Decision("00", "Q7R2K9", "THANK YOU"));
        assertEquals("00", o.rc());
        assertEquals("Q7R2K9", o.response().getString(38));
        assertEquals("THANK YOU", o.response().getString(44));
        assertEquals(TransactionConstants.PREPARED, o.vote() & TransactionConstants.PREPARED);
    }

    @Test
    void aDeclineCarriesItsMessage() throws Exception {
        Outcome o = run(EIGHTY_SEVEN, new CoreAnswer.Decision("51", null, "INSUFFICIENT FUNDS"));
        assertEquals("51", o.rc());
        assertEquals("INSUFFICIENT FUNDS", o.response().getString(44));
        assertFalse(o.response().hasField(38));
    }

    @Test
    void anApprovalCodeThatDoesNotFitEndsAsASystemError() throws Exception {
        Outcome o = run(EIGHTY_SEVEN, new CoreAnswer.Decision("00", "Q7R2K9XY", "THANK YOU"));
        assertEquals("96", o.rc());
        assertFalse(o.response().hasField(38), "no approval code on a 96");
        assertEquals(0, o.vote() & TransactionConstants.PREPARED);
    }

    @Test
    void aLongMessageDoesNotTurnADeclineIntoSomethingElse() throws Exception {
        Outcome o = run(EIGHTY_SEVEN, new CoreAnswer.Decision("51", null,
                "INSUFFICIENT FUNDS - CALL YOUR BRANCH"));
        assertEquals("51", o.rc());
        assertFalse(o.response().hasField(44));
    }

    @Test
    void aLongMessageDoesNotCostTheApproval() throws Exception {
        Outcome o = run(EIGHTY_SEVEN, new CoreAnswer.Decision("00", "Q7R2K9",
                "APPROVED - THANK YOU FOR SHOPPING"));
        assertEquals("00", o.rc());
        assertEquals("Q7R2K9", o.response().getString(38));
        assertFalse(o.response().hasField(44));
    }

    @Test
    void aLinkWithRoomForTheMessageGetsAllOfIt() throws Exception {
        Outcome o = run(NINETY_THREE, new CoreAnswer.Decision("51", null,
                "INSUFFICIENT FUNDS - CALL YOUR BRANCH"));
        assertEquals("INSUFFICIENT FUNDS - CALL YOUR BRANCH", o.response().getString(44));
    }
}
