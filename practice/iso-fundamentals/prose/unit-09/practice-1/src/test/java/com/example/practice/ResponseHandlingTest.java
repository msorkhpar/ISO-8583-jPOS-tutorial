package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.practice.ResponseHandling.Action;
import com.example.practice.ResponseHandling.Outcome;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class ResponseHandlingTest {

    private static ISOMsg answer(String code) throws Exception {
        ISOMsg response = new ISOMsg();
        response.setMTI("0110");
        if (code != null) {
            response.set(39, code);
        }
        return response;
    }

    @Test
    void sortsCodesAndErrorsIntoTheirHandling() throws Exception {
        assertEquals(Outcome.APPROVED, ResponseHandling.outcomeOf(answer("00")));
        assertEquals(Outcome.DECLINED, ResponseHandling.outcomeOf(answer("05")));
        assertEquals(Outcome.DECLINED, ResponseHandling.outcomeOf(answer("51")));
        assertEquals(Outcome.DECLINED, ResponseHandling.outcomeOf(answer("91")));
        assertEquals(Outcome.REFER, ResponseHandling.outcomeOf(answer("01")));
        assertEquals(Outcome.REFER, ResponseHandling.outcomeOf(answer("02")));

        assertEquals(Action.RETRY, ResponseHandling.actionFor(new IOException("channel closed")));
        assertEquals(Action.LOG_FOR_FIX, ResponseHandling.actionFor(new ISOException("field 4 too long")));
        assertEquals(Action.ALERT_SECURITY, ResponseHandling.actionFor(new SecurityException("MAC mismatch")));
    }

    @Test
    void partialApprovalAndNoReasonToDeclineAreApprovals() throws Exception {
        assertEquals(Outcome.APPROVED, ResponseHandling.outcomeOf(answer("10")));
        assertEquals(Outcome.APPROVED, ResponseHandling.outcomeOf(answer("85")));
    }

    @Test
    void aResponseWithoutACodeIsDeclined() throws Exception {
        assertEquals(Outcome.DECLINED, ResponseHandling.outcomeOf(answer(null)));
    }

    @Test
    void aTimeoutIsANetworkError() {
        assertEquals(Action.RETRY, ResponseHandling.actionFor(new SocketTimeoutException("read timed out")));
    }
}
