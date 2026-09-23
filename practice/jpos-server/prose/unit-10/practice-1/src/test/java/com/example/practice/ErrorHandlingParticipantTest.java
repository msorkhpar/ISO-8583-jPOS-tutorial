package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.junit.jupiter.api.Test;

class ErrorHandlingParticipantTest {

    private static final int ABORTED = TransactionParticipant.ABORTED | TransactionParticipant.NO_JOIN;

    private static Context withRequest(Object request) {
        Context ctx = new Context();
        if (request != null) {
            ctx.put("REQUEST", request);
        }
        return ctx;
    }

    private static ISOMsg purchase(String processingCode) throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        msg.set(3, processingCode);
        return msg;
    }

    private static void assertResponse(String code, String text, ISOMsg response) {
        assertEquals(code, response.getString(39));
        assertEquals(text, response.getString(63));
    }

    @Test
    void eachKindOfFailureGetsItsOwnCodeAndMessage() {
        assertResponse("01", "bad bitmap", ErrorHandlingParticipant.errorResponse(
                new ErrorHandlingParticipant.ISO8583ParseException("bad bitmap")));
        assertResponse("02", "limit exceeded", ErrorHandlingParticipant.errorResponse(
                new ErrorHandlingParticipant.TransactionProcessingException("limit exceeded")));
        assertResponse("03", "host down", ErrorHandlingParticipant.errorResponse(
                new ErrorHandlingParticipant.NetworkCommunicationException("host down")));
    }

    @Test
    void theParticipantAbortsWithAnErrorResponseInsteadOfThrowing() throws Exception {
        ErrorHandlingParticipant participant = new ErrorHandlingParticipant();

        Context good = withRequest(purchase("000000"));
        assertEquals(TransactionParticipant.PREPARED | TransactionParticipant.NO_JOIN,
                participant.prepare(1L, good));
        assertNull(good.get("RESPONSE"));

        Context missing = withRequest(null);
        assertEquals(ABORTED, participant.prepare(2L, missing));
        assertResponse("01", "Invalid or missing ISO message", (ISOMsg) missing.get("RESPONSE"));

        Context processing = withRequest(purchase("999999"));
        assertEquals(ABORTED, participant.prepare(3L, processing));
        assertResponse("02", "Invalid processing code", (ISOMsg) processing.get("RESPONSE"));

        Context network = withRequest(purchase("888888"));
        assertEquals(ABORTED, participant.prepare(4L, network));
        assertResponse("03", "Network is unavailable", (ISOMsg) network.get("RESPONSE"));
    }

    @Test
    void anUnexpectedFailureSaysNothingAboutItsInternals() {
        ISOMsg response = ErrorHandlingParticipant.errorResponse(
                new IllegalStateException("select * from keys where alias='zmk-prod'"));
        assertResponse("99", "An unexpected error occurred", response);
        assertFalse(response.getString(63).contains("zmk-prod"));
    }

    @Test
    void aRequestOfTheWrongTypeStillAbortsCleanly() {
        ErrorHandlingParticipant participant = new ErrorHandlingParticipant();
        Context ctx = withRequest("0200 not a message");
        assertEquals(ABORTED, participant.prepare(1L, ctx));
        assertEquals("99", ((ISOMsg) ctx.get("RESPONSE")).getString(39));
    }
}
