package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonPackagerTest {

    private static ISOMsg unpack(byte[] json) throws Exception {
        ISOMsg m = new ISOMsg();
        m.setPackager(new JsonPackager());
        m.unpack(json);
        return m;
    }

    private static ISOMsg roundTrip(ISOMsg sent) throws Exception {
        sent.setPackager(new JsonPackager());
        return unpack(sent.pack());
    }

    @Test
    void readsThePartnersMessage() throws Exception {
        String fromPartner = "{ \"0\": \"0110\", \"2\": \"5555555555554444\", \"4\": \"000000001999\","
                + " \"11\": \"000731\", \"39\": \"00\", \"41\": \"29110001\" }";
        ISOMsg m = unpack(fromPartner.getBytes(StandardCharsets.UTF_8));
        assertEquals("0110", m.getMTI());
        assertEquals("5555555555554444", m.getString(2));
        assertEquals("000000001999", m.getString(4));
        assertEquals("000731", m.getString(11));
        assertEquals("00", m.getString(39));
        assertEquals("29110001", m.getString(41));
    }

    @Test
    void aMessageComesBackWithItsCardTerminalAndMerchant() throws Exception {
        ISOMsg sent = new ISOMsg("0110");
        sent.set(2, "5555555555554444");
        sent.set(41, "29110001");
        sent.set(42, "SHOP00000004471");
        ISOMsg back = roundTrip(sent);
        assertEquals("5555555555554444", back.getString(2));
        assertEquals("29110001", back.getString(41));
        assertEquals("SHOP00000004471", back.getString(42));
    }

    @Test
    void digitsKeepTheirZeros() throws Exception {
        ISOMsg sent = new ISOMsg("0110");
        sent.set(4, "000000001999");
        sent.set(11, "000731");
        sent.set(38, "000917");
        sent.set(39, "00");
        ISOMsg back = roundTrip(sent);
        assertEquals("0110", back.getMTI());
        assertEquals("000000001999", back.getString(4));
        assertEquals("000731", back.getString(11));
        assertEquals("000917", back.getString(38));
        assertEquals("00", back.getString(39));
    }

    @Test
    void bytesComeBackByteForByte() throws Exception {
        byte[] pinBlock = ISOUtil.hex2byte("9C3A00F17B44E205");
        byte[] chip = ISOUtil.hex2byte("9F2608A1B2C3D4E5F607189F360200C1");
        ISOMsg sent = new ISOMsg("0100");
        sent.set(11, "000732");
        sent.set(52, pinBlock);
        sent.set(55, chip);
        ISOMsg back = roundTrip(sent);
        assertArrayEquals(pinBlock, back.getBytes(52));
        assertArrayEquals(chip, back.getBytes(55));
    }

    @Test
    void quotesAndBackslashesSurvive() throws Exception {
        ISOMsg sent = new ISOMsg("0100");
        sent.set(11, "000733");
        sent.set(43, "BOB'S \"FIX-IT\" SHOP     CENTERVILLE  US");
        sent.set(48, "TERM\\LANE 04");
        ISOMsg back = roundTrip(sent);
        assertEquals("BOB'S \"FIX-IT\" SHOP     CENTERVILLE  US", back.getString(43));
        assertEquals("TERM\\LANE 04", back.getString(48));
    }
}
