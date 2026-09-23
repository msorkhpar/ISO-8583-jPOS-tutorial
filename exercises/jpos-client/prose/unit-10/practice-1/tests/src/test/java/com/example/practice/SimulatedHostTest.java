package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class SimulatedHostTest {

    private static ISOMsg authorization(String amount) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0100");
        msg.set(2, "4111111111111111");
        msg.set(4, amount);
        msg.set(11, "000042");
        return msg;
    }

    @Test
    void aRequestWithinTheLimitIsApprovedWithItsFieldsEchoed() throws Exception {
        SimulatedHost host = new SimulatedHost(50000);
        assertTrue(host.isConnected());
        ISOMsg reply = host.request(authorization("000000010000"), 30000);
        assertNotNull(reply);
        assertEquals("0110", reply.getMTI());
        assertEquals("4111111111111111", reply.getString(2));
        assertEquals("000000010000", reply.getString(4));
        assertEquals("000042", reply.getString(11));
        assertEquals("00", reply.getString(39));
    }

    @Test
    void aRequestOverTheLimitIsDeclinedWith51() throws Exception {
        SimulatedHost host = new SimulatedHost(50000);
        assertEquals("51", host.request(authorization("000000050001"), 30000).getString(39));
        assertEquals("00", host.request(authorization("000000050000"), 30000).getString(39));
    }

    @Test
    void theRequestItselfIsLeftUntouched() throws Exception {
        ISOMsg request = authorization("000000010000");
        new SimulatedHost(50000).request(request, 30000);
        assertEquals("0100", request.getMTI());
        assertFalse(request.hasField(39));
    }

    @Test
    void aRequestWithoutAnAmountIsApproved() throws Exception {
        ISOMsg echo = new ISOMsg();
        echo.setMTI("0800");
        echo.set(11, "000043");
        echo.set(70, "301");
        ISOMsg reply = new SimulatedHost(50000).request(echo, 30000);
        assertEquals("0810", reply.getMTI());
        assertEquals("301", reply.getString(70));
        assertEquals("00", reply.getString(39));
    }
}
