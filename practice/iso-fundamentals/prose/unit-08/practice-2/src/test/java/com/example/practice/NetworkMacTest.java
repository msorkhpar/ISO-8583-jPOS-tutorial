package com.example.practice;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.macs.ISO9797Alg3Mac;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkMacTest {
    /** A made-up test ZAK. */
    private static final byte[] ZAK = ISOUtil.hex2byte("0F1E2D3C4B5A69788796A5B4C3D2E1F0");

    /** The network's own reference: Bouncy Castle's ISO 9797-1 algorithm 3 with padding method 2. */
    private static byte[] networkMac(byte[] data) {
        ISO9797Alg3Mac mac = new ISO9797Alg3Mac(new DESEngine(), new ISO7816d4Padding());
        mac.init(new KeyParameter(ZAK));
        mac.update(data, 0, data.length);
        byte[] out = new byte[8];
        mac.doFinal(out, 0);
        return out;
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void computesTheNetworksMac() throws Exception {
        byte[] echo = ascii("0800822000000000000004000000000000001106093015000917301");    // 55 bytes
        byte[] before = echo.clone();
        assertArrayEquals(networkMac(echo), NetworkMac.mac(ZAK, echo));
        assertArrayEquals(before, echo, "the MAC input is left as it was");
        byte[] purchase = ascii("0200302000000080000000000000000123450009172911000201");      // 52 bytes
        assertArrayEquals(networkMac(purchase), NetworkMac.mac(ZAK, purchase));
    }

    @Test
    void acceptsOnlyTheRightMac() throws Exception {
        byte[] purchase = ascii("0200302000000080000000000000000123450009172911000201");
        byte[] right = networkMac(purchase);
        assertTrue(NetworkMac.verify(ZAK, purchase, right.clone()));

        byte[] oneBitOff = right.clone();
        oneBitOff[7] ^= 0x01;
        assertFalse(NetworkMac.verify(ZAK, purchase, oneBitOff), "a MAC with one bit changed");

        byte[] otherAmount = ascii("0200302000000080000000000000001234500009172911000201");
        assertFalse(NetworkMac.verify(ZAK, otherAmount, right.clone()), "the amount changed on the way");
        assertFalse(NetworkMac.verify(ZAK, purchase, null), "a message with no MAC");
    }

    @Test
    void wholeBlocksStillGetPadding() throws Exception {
        byte[] sixteen = ascii("0800822000000000");
        byte[] twentyFour = ascii("080082200000000000000400");
        assertEquals(16, sixteen.length);
        assertArrayEquals(networkMac(sixteen), NetworkMac.mac(ZAK, sixteen));
        assertArrayEquals(networkMac(twentyFour), NetworkMac.mac(ZAK, twentyFour));
    }

    @Test
    void refusesAMacCutShort() throws Exception {
        byte[] purchase = ascii("0200302000000080000000000000000123450009172911000201");
        byte[] right = networkMac(purchase);
        assertFalse(NetworkMac.verify(ZAK, purchase, Arrays.copyOf(right, 4)), "only the first four bytes");
        assertFalse(NetworkMac.verify(ZAK, purchase, new byte[0]), "an empty MAC field");
    }
}
