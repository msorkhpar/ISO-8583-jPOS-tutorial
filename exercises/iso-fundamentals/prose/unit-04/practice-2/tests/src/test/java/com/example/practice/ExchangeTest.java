package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class ExchangeTest {

    private static ISOMsg message(String mti, String stan, String responseCode) throws Exception {
        ISOMsg message = new ISOMsg();
        message.setMTI(mti);
        message.set(11, stan);
        if (responseCode != null) {
            message.set(39, responseCode);
        }
        return message;
    }

    @Test
    void readsTheOutcomeOfAResponseThatAnswersItsRequest() throws Exception {
        assertEquals(Exchange.Outcome.APPROVED,
            Exchange.outcome(message("0100", "000123", null), message("0110", "000123", "00")));
        assertEquals(Exchange.Outcome.DECLINED,
            Exchange.outcome(message("0200", "000124", null), message("0210", "000124", "51")));
        assertEquals(Exchange.Outcome.DECLINED,
            Exchange.outcome(message("0400", "000125", null), message("0410", "000125", "05")));
        assertEquals(Exchange.Outcome.APPROVED,
            Exchange.outcome(message("0800", "000126", null), message("0810", "000126", "00")));
        // An echo of the request is not a response.
        assertThrows(IllegalArgumentException.class,
            () -> Exchange.outcome(message("0100", "000127", null), message("0100", "000127", "00")));
        // A response is not a request that can be answered.
        assertThrows(IllegalArgumentException.class,
            () -> Exchange.outcome(message("0110", "000128", "00"), message("0110", "000128", "00")));
        // No response code, no decision.
        assertThrows(IllegalArgumentException.class,
            () -> Exchange.outcome(message("0100", "000129", null), message("0110", "000129", null)));
    }

    @Test
    void aResponseOfAnotherClassIsNotTheAnswer() throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> Exchange.outcome(message("0100", "000130", null), message("0210", "000130", "00")));
        assertThrows(IllegalArgumentException.class,
            () -> Exchange.outcome(message("0400", "000131", null), message("0810", "000131", "00")));
    }

    @Test
    void aResponseWithAnotherStanIsNotTheAnswer() throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> Exchange.outcome(message("0100", "000132", null), message("0110", "000999", "00")));
    }
}
