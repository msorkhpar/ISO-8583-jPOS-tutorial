package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class HostSimulatorTest {

    private static ISOMsg request(String mti, String pan) throws Exception {
        ISOMsg m = new ISOMsg();
        m.setMTI(mti);
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, "000000010000");
        m.set(11, "000123");
        return m;
    }

    @Test
    void approvesARequestWithACopyThatCarriesTheResponseMti() throws Exception {
        HostSimulator host = new HostSimulator("123456");
        ISOMsg answer = host.respond(request("0100", "4111111111111111"));
        assertEquals("0110", answer.getMTI());
        assertEquals("00", answer.getString(39));
        assertEquals("123456", answer.getString(38));
        assertEquals("4111111111111111", answer.getString(2));
        assertEquals("000000010000", answer.getString(4));
        assertEquals("000123", answer.getString(11));
        assertEquals("0210", host.respond(request("0200", "4111111111111111")).getMTI());
    }

    @Test
    void declinesATestCardWithItsCodeAndNoApprovalCode() throws Exception {
        HostSimulator host = new HostSimulator("123456");
        host.declineCard("5555555555554444", "05");
        ISOMsg answer = host.respond(request("0100", "5555555555554444"));
        assertEquals("0110", answer.getMTI());
        assertEquals("05", answer.getString(39));
        assertFalse(answer.hasField(38));
        assertEquals("00", host.respond(request("0100", "4111111111111111")).getString(39));
    }

    @Test
    void leavesTheRequestUntouched() throws Exception {
        HostSimulator host = new HostSimulator("123456");
        ISOMsg sent = request("0100", "4111111111111111");
        host.respond(sent);
        assertEquals("0100", sent.getMTI());
        assertFalse(sent.hasField(38));
        assertFalse(sent.hasField(39));
    }

    @Test
    void refusesToAnswerAResponse() throws Exception {
        HostSimulator host = new HostSimulator("123456");
        ISOMsg alreadyAnswered = request("0110", "4111111111111111");
        assertThrows(ISOException.class, () -> host.respond(alreadyAnswered));
    }
}
