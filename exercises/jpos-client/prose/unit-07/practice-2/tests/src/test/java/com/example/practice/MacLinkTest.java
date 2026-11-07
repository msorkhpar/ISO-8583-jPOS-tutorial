package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.security.SMAdapter;
import org.jpos.security.SecureDESKey;
import org.jpos.security.jceadapter.JCESecurityModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MacLinkTest {
    static final String MAC_KEY = "4A5B6C7D8E9FA0B1C2D3E4F506172839";   // a clear test key for this practice
    static ISOPackager packager;

    @BeforeAll
    static void loadPackager() throws Exception {
        packager = new GenericPackager("jar:packager/iso87ascii.xml");
    }

    static JCESecurityModule module() throws Exception {
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("provider", "org.bouncycastle.jce.provider.BouncyCastleProvider");
        cfg.put("rebuildlmk", "true");
        JCESecurityModule m = new JCESecurityModule();
        m.setConfiguration(cfg);
        return m;
    }

    /** The host's side of the spec, written out on its own: 64, or 128 with fields above 64. */
    static final class HostMac {
        final JCESecurityModule hsm = module();
        final SecureDESKey key = hsm.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_ZAK, MAC_KEY);

        HostMac() throws Exception { }

        static int field(ISOMsg m) {
            for (int f = 65; f <= 127; f++)
                if (m.hasField(f)) return 128;
            return 64;
        }

        byte[] mac(ISOMsg m) throws Exception {
            ISOMsg c = (ISOMsg) m.clone();
            c.setPackager(packager);
            c.set(field(m), new byte[8]);
            byte[] p = c.pack();
            return hsm.generateCBC_MAC(Arrays.copyOf(p, p.length - 16), key);   // 8 bytes travel as 16 hex digits
        }

        boolean verifies(ISOMsg m) throws Exception {
            byte[] got = m.getBytes(field(m));
            return got != null && MessageDigest.isEqual(got, mac(m));
        }
    }

    /** The host behind a MUX: checks the MAC, declines amounts over 500.00, signs its answer. */
    static final class Host implements MUX {
        final HostMac macs = new HostMac();
        final List<ISOMsg> received = new ArrayList<>();
        final List<Boolean> verified = new ArrayList<>();
        UnaryOperator<ISOMsg> onTheWayBack = r -> r;

        Host() throws Exception { }

        @Override
        public ISOMsg request(ISOMsg m, long timeout) {
            try {
                received.add((ISOMsg) m.clone());
                boolean ok = macs.verifies(m);
                verified.add(ok);
                ISOMsg r = (ISOMsg) m.clone(2, 3, 4, 11, 41, 90);
                r.setPackager(packager);
                r.setMTI(m.getMTI());
                r.setResponseMTI();
                r.set(39, !ok ? "63" : Long.parseLong(m.getString(4)) > 50000 ? "51" : "00");
                r.set(HostMac.field(r), macs.mac(r));
                return onTheWayBack.apply(r);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override public void request(ISOMsg m, long t, ISOResponseListener l, Object h) { throw new UnsupportedOperationException(); }
        @Override public void send(ISOMsg m) { throw new UnsupportedOperationException(); }
        @Override public boolean isConnected() { return true; }
    }

    static MacLink link(Host host) throws Exception {
        JCESecurityModule ours = module();
        return new MacLink(host, ours,
                ours.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_ZAK, MAC_KEY));
    }

    static ISOMsg purchase(String stan, String amount) throws ISOException {
        ISOMsg m = new ISOMsg("0200");
        m.setPackager(packager);
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "29110005");
        return m;
    }

    @Test
    void aSignedPurchaseIsApproved() throws Exception {
        Host host = new Host();
        assertEquals(MacLink.Outcome.APPROVED, link(host).send(purchase("000871", "000000014999"), 30000));
        assertEquals(List.of(true), host.verified, "the host verified the request's MAC");
    }

    @Test
    void aDeclineThatVerifiesIsADecline() throws Exception {
        Host host = new Host();
        ISOMsg big = purchase("000872", "000000080000");
        assertEquals(MacLink.Outcome.DECLINED, link(host).send(big, 30000));
        assertEquals(List.of(true), host.verified);
        assertFalse(big.hasField(64), "the caller's request is not changed");
    }

    @Test
    void aReversalCarriesItsMacInField128() throws Exception {
        Host host = new Host();
        ISOMsg reversal = new ISOMsg("0420");
        reversal.setPackager(packager);
        reversal.set(2, "4111111111111111");
        reversal.set(3, "000000");
        reversal.set(4, "000000003310");
        reversal.set(11, "000874");
        reversal.set(41, "29110005");
        reversal.set(90, "020000087311061615220000000765400000000000");
        assertEquals(MacLink.Outcome.APPROVED, link(host).send(reversal, 30000));
        ISOMsg sent = host.received.get(0);
        assertFalse(sent.hasField(64), "a message with fields above 64 has no field 64");
        assertTrue(sent.hasField(128));
        assertEquals(List.of(true), host.verified);
    }

    @Test
    void anApprovalChangedOnTheWayBackIsUnknown() throws Exception {
        Host host = new Host();
        host.onTheWayBack = r -> { r.set(4, "000000000100"); return r; };
        assertEquals(MacLink.Outcome.UNKNOWN, link(host).send(purchase("000875", "000000022750"), 30000));
    }

    @Test
    void anApprovalThatArrivesWithoutItsMacIsUnknown() throws Exception {
        Host host = new Host();
        host.onTheWayBack = r -> { r.unset(64); return r; };
        assertEquals(MacLink.Outcome.UNKNOWN, link(host).send(purchase("000876", "000000022750"), 30000));
    }

    @Test
    void noAnswerIsUnknown() throws Exception {
        Host host = new Host();
        host.onTheWayBack = r -> null;
        assertEquals(MacLink.Outcome.UNKNOWN, link(host).send(purchase("000877", "000000022750"), 30000));
        assertEquals(1, host.received.size(), "the request went out once");
    }
}
