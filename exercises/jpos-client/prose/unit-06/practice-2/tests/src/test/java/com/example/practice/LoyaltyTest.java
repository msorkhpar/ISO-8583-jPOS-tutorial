package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoyaltyTest {

    private static byte[] pack(ISOMsg m) throws Exception {
        ISOMsg out = (ISOMsg) m.clone();
        out.setPackager(Loyalty.packager());
        return out.pack();
    }

    private static ISOMsg unpack(byte[] wire) throws Exception {
        GenericPackager p = Loyalty.packager();
        ISOMsg in = new ISOMsg();
        in.setPackager(p);
        in.unpack(wire);
        return in;
    }

    /** The bytes of field 63 as the host receives them, length prefix included (it is the last field). */
    private static String field63OnTheWire(ISOMsg m) throws Exception {
        String wire = new String(pack(m), StandardCharsets.ISO_8859_1);
        ISOMsg copy = (ISOMsg) m.clone();
        copy.unset(63);
        String without = new String(pack(copy), StandardCharsets.ISO_8859_1);
        // the same fields before 63 and a bitmap of the same size, so what is left over is field 63
        return wire.substring(without.length());
    }

    private static ISOMsg response(String balance) throws Exception {
        ISOMsg r = new ISOMsg("0210");
        r.set(11, "007312");
        r.set(39, "00");
        r.set(41, "29110001");
        if (balance != null) {
            r.set("63.1", "FU");
            r.set("63.2", "60119004218");
            r.set("63.3", "000000");
            r.set("63.4", balance);
        }
        return r;
    }

    @Test
    void aMembersPurchaseCarriesTheLoyaltyData() throws Exception {
        ISOMsg m = Loyalty.purchase("4111111111111111", 6420, "007311", "FU", "60119004218", 0);
        ISOMsg atHost = unpack(pack(m));
        assertEquals("FU", atHost.getString("63.1"));
        assertEquals("60119004218", atHost.getString("63.2").trim());
        assertEquals("000000006420", atHost.getString(4));
    }

    @Test
    void theHostsBalanceIsRead() throws Exception {
        ISOMsg received = unpack(pack(response("00012840")));
        assertEquals("00012840", Loyalty.pointsBalance(received));
    }

    @Test
    void theMemberNumberTravelsWithItsLength() throws Exception {
        ISOMsg m = Loyalty.purchase("5555555555554444", 1500, "007313", "GR", "7710342", 0);
        assertEquals("011GR077710342", field63OnTheWire(m));
    }

    @Test
    void pointsToRedeemSurviveTheMemberStep() throws Exception {
        ISOMsg m = Loyalty.purchase("5555555555554444", 8800, "007314", "GR", "7710342", 350);
        ISOMsg atHost = unpack(pack(m));
        assertEquals("000350", atHost.getString("63.3"));
        assertEquals("GR", atHost.getString("63.1"));
        assertTrue(atHost.getString("63.2").trim().equals("7710342"));
    }

    @Test
    void aResponseForANonMemberHasNoBalance() throws Exception {
        ISOMsg received = unpack(pack(response(null)));
        assertNull(Loyalty.pointsBalance(received));
    }
}
