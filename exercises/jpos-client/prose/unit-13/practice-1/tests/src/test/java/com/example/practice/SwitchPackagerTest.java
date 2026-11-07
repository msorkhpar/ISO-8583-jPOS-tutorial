package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

class SwitchPackagerTest {

    /** Reads one of the switch's sample messages with the packager, failing on any byte left over. */
    private static ISOMsg read(String sampleHex) throws ISOException {
        byte[] sample = ISOUtil.hex2byte(sampleHex);
        ISOMsg m = new ISOMsg();
        m.setPackager(new SwitchPackager());
        int used;
        try {
            used = m.unpack(sample);
        } catch (ISOException e) {
            throw new AssertionError("the switch's sample does not unpack: " + e.getMessage(), e);
        }
        assertEquals(sample.length, used, "bytes of the sample left unread");
        return m;
    }

    /** Packs the message again with the packager and compares it with the sample. */
    private static void packsBackTo(ISOMsg m, String sampleHex) throws ISOException {
        m.setPackager(new SwitchPackager());
        assertEquals(sampleHex, ISOUtil.hexString(m.pack()), "the packed bytes differ from the sample");
    }

    @Test
    void aMessageReadsBackWhatWasPacked() throws Exception {
        SwitchPackager packager = new SwitchPackager();
        ISOMsg m = new ISOMsg("0200");
        m.setPackager(packager);
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000004321");
        m.set(11, "000917");
        m.set(41, "POS00933");
        m.set(52, ISOUtil.hex2byte("3B90C4D21E7F6A05"));
        m.set(70, "002");
        ISOMsg back = new ISOMsg();
        back.setPackager(packager);
        back.unpack(m.pack());
        for (int f : new int[] { 2, 3, 4, 11, 41, 70 })
            assertEquals(m.getString(f), back.getString(f), "field " + f);
        assertArrayEquals(m.getBytes(52), back.getBytes(52), "field 52");
    }

    @Test
    void theSampleSignOnCarriesCode001() throws Exception {
        String sample = "0800802000000000000004000000000000000009140010";
        ISOMsg m = read(sample);
        assertEquals("001", m.getString(70));
        packsBackTo(m, sample);
    }

    @Test
    void theSamplePurchaseCarriesItsCardNumber() throws Exception {
        String sample = "02007020000000000000165555555555554444000000000000012999000913";
        ISOMsg m = read(sample);
        assertEquals("5555555555554444", m.getString(2));
        assertEquals("000000012999", m.getString(4));
        packsBackTo(m, sample);
    }

    @Test
    void theSampleEchoCarriesItsTerminal() throws Exception {
        String sample = "08000020000000800000000915C1E3D4F0F0F4F1F7";
        ISOMsg m = read(sample);
        assertEquals("ATM00417", m.getString(41));
        packsBackTo(m, sample);
    }

    @Test
    void theSamplePinBlockIsEightBytes() throws Exception {
        String sample = "020000200000000010000009168E2C51A07B3D9F46";
        ISOMsg m = read(sample);
        assertArrayEquals(ISOUtil.hex2byte("8E2C51A07B3D9F46"), m.getBytes(52));
        packsBackTo(m, sample);
    }
}
