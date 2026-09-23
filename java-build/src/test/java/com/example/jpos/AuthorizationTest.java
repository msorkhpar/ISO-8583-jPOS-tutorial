package com.example.jpos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.ISO87APackager;
import org.junit.jupiter.api.Test;

class AuthorizationTest {

    @Test
    void aPackedRequestUnpacksToTheSameFields() throws Exception {
        ISOMsg sent = Authorization.request("4111111111111111", "000000010000", "000001");

        ISOMsg received = new ISOMsg();
        received.setPackager(new ISO87APackager());
        received.unpack(Authorization.pack(sent));

        assertEquals("0100", received.getMTI());
        assertEquals("4111111111111111", received.getString(2));
        assertEquals("000000010000", received.getString(4));
        assertEquals("000001", received.getString(11));
    }

    @Test
    void theWireFormOpensWithTheMti() throws Exception {
        byte[] wire = Authorization.pack(
            Authorization.request("4111111111111111", "000000010000", "000001"));

        assertTrue(new String(wire, java.nio.charset.StandardCharsets.US_ASCII).startsWith("0100"));
    }
}
