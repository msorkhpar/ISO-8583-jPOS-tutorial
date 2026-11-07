package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.jpos.security.EncryptedPIN;
import org.jpos.security.SMAdapter;
import org.jpos.security.SMException;
import org.jpos.security.SecureDESKey;
import org.jpos.security.jceadapter.JCESecurityModule;
import org.junit.jupiter.api.Test;

class PinRelayTest {
    // Clear test keys for this practice only.
    static final String OLD_MODEL_KEY = "0F1E2D3C4B5A6978A1B2C3D4E5F60718";
    static final String NEW_MODEL_KEY = "5B4A39281706F5E4D3C2B1A098877665";
    static final String HOST_KEY      = "3C4D5E6F70819203A4B5C6D7E8F90A1B";
    static final String STALE_KEY     = "7766554433221100FFEEDDCCBBAA9988";

    static final String OLD_MODEL = "29110007", NEW_MODEL = "29110008";
    static final Map<String, String> PINS = Map.of("4111111111111111", "4096", "5555555555554444", "739154");

    static JCESecurityModule module() throws Exception {
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("provider", "org.bouncycastle.jce.provider.BouncyCastleProvider");
        cfg.put("rebuildlmk", "true");
        JCESecurityModule m = new JCESecurityModule();
        m.setConfiguration(cfg);
        return m;
    }

    static SecureDESKey key(JCESecurityModule m, String type, String clear) throws SMException {
        return m.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, type, clear);
    }

    /** The PIN pad: makes the block a terminal puts in field 52. */
    static byte[] pinBlock(String pin, String pan, String clearKey, byte format) throws Exception {
        JCESecurityModule pad = module();
        return pad.exportPIN(pad.encryptPIN(pin, pan), key(pad, SMAdapter.TYPE_TPK, clearKey), format).getPINBlock();
    }

    /** The acquirer's host behind a MUX: reads ISO format 0 under its key, answers 00 or 55. */
    static final class Host implements MUX {
        final JCESecurityModule hsm = module();
        final SecureDESKey zpk = key(hsm, SMAdapter.TYPE_ZPK, HOST_KEY);
        final List<ISOMsg> received = new ArrayList<>();
        final List<String> pinsRead = new ArrayList<>();

        Host() throws Exception { }

        @Override
        public ISOMsg request(ISOMsg m, long timeout) {
            received.add((ISOMsg) m.clone());
            try {
                ISOMsg r = (ISOMsg) m.clone(2, 3, 4, 11, 35, 41);
                r.setMTI(m.getMTI());
                r.setResponseMTI();
                r.set(39, decide(m));
                return r;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private String decide(ISOMsg m) throws Exception {
            if (!m.hasField(52))
                return "00";
            String pan = m.hasField(2) ? m.getString(2) : m.getString(35).split("=")[0];
            try {
                String pin = hsm.decryptPIN(hsm.importPIN(
                        new EncryptedPIN(m.getBytes(52), SMAdapter.FORMAT01, pan), zpk));
                pinsRead.add(pin);
                return pin.equals(PINS.get(pan)) ? "00" : "55";
            } catch (SMException e) {
                pinsRead.add("unreadable");
                return "55";
            }
        }

        @Override public void request(ISOMsg m, long t, ISOResponseListener l, Object h) { throw new UnsupportedOperationException(); }
        @Override public void send(ISOMsg m) { throw new UnsupportedOperationException(); }
        @Override public boolean isConnected() { return true; }
    }

    static final class Shop {
        final JCESecurityModule hsm = module();
        final Host host = new Host();
        final PinRelay relay = new PinRelay(hsm, Map.of(
                OLD_MODEL, new PinRelay.Terminal(key(hsm, SMAdapter.TYPE_TPK, OLD_MODEL_KEY), SMAdapter.FORMAT01),
                NEW_MODEL, new PinRelay.Terminal(key(hsm, SMAdapter.TYPE_TPK, NEW_MODEL_KEY), SMAdapter.FORMAT05)),
                key(hsm, SMAdapter.TYPE_ZPK, HOST_KEY), host);

        Shop() throws Exception { }
    }

    static ISOMsg purchase(String terminal, String stan, String amount) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    static ISOMsg chip(String terminal, String stan, String pan, byte[] block) throws Exception {
        ISOMsg m = purchase(terminal, stan, "000000006150");
        m.set(2, pan);
        m.set(52, block);
        return m;
    }

    @Test
    void aChipPurchaseWithItsPinIsApproved() throws Exception {
        Shop shop = new Shop();
        byte[] block = pinBlock("4096", "4111111111111111", OLD_MODEL_KEY, SMAdapter.FORMAT01);
        ISOMsg answer = shop.relay.send(chip(OLD_MODEL, "004417", "4111111111111111", block), 30000);
        assertNotNull(answer);
        assertEquals("00", answer.getString(39));
        assertEquals(List.of("4096"), shop.host.pinsRead, "the host read the PIN the cardholder typed");
    }

    @Test
    void theHostGetsThePurchaseAsTheTerminalSentItApartFromThePin() throws Exception {
        Shop shop = new Shop();
        byte[] block = pinBlock("739154", "5555555555554444", OLD_MODEL_KEY, SMAdapter.FORMAT01);
        ISOMsg fromTerminal = chip(OLD_MODEL, "004418", "5555555555554444", block);
        shop.relay.send(fromTerminal, 30000);
        ISOMsg sent = shop.host.received.get(0);
        for (int f : new int[] { 2, 3, 4, 11, 41 })
            assertEquals(fromTerminal.getString(f), sent.getString(f), "field " + f);
        assertEquals(8, sent.getBytes(52).length, "field 52 holds the 8 bytes of the block");
        assertFalse(Arrays.equals(block, sent.getBytes(52)), "the terminal's block is not the host's");
        assertArrayEquals(block, fromTerminal.getBytes(52), "the terminal's own request is left as it came");

        ISOMsg noPin = purchase(OLD_MODEL, "004419", "000000000875");
        noPin.set(2, "4111111111111111");
        assertEquals("00", shop.relay.send(noPin, 30000).getString(39));
        assertFalse(shop.host.received.get(1).hasField(52));
    }

    @Test
    void aSwipedCardArrivesWithOnlyItsTrack() throws Exception {
        Shop shop = new Shop();
        ISOMsg swiped = purchase(OLD_MODEL, "004420", "000000002299");
        swiped.set(35, "5555555555554444=29071010000012345");
        swiped.set(52, pinBlock("739154", "5555555555554444", OLD_MODEL_KEY, SMAdapter.FORMAT01));
        ISOMsg answer = shop.relay.send(swiped, 30000);
        assertNotNull(answer);
        assertEquals("00", answer.getString(39));
        assertEquals(List.of("739154"), shop.host.pinsRead);
    }

    @Test
    void theNewTerminalModelSendsIsoFormat1() throws Exception {
        Shop shop = new Shop();
        byte[] block = pinBlock("4096", "4111111111111111", NEW_MODEL_KEY, SMAdapter.FORMAT05);
        ISOMsg answer = shop.relay.send(chip(NEW_MODEL, "000052", "4111111111111111", block), 30000);
        assertNotNull(answer);
        assertEquals("00", answer.getString(39));
        assertEquals(List.of("4096"), shop.host.pinsRead, "the host reads only ISO format 0");
    }

    @Test
    void aTerminalWithAStaleKeyGetsNothingPastTheRelay() throws Exception {
        Shop shop = new Shop();
        byte[] block = pinBlock("4096", "4111111111111111", STALE_KEY, SMAdapter.FORMAT01);
        ISOMsg answer = shop.relay.send(chip(OLD_MODEL, "004421", "4111111111111111", block), 30000);
        assertNotNull(answer, "the terminal still gets an answer");
        assertEquals("0210", answer.getMTI());
        assertFalse("00".equals(answer.getString(39)), "the purchase is not approved");
        assertTrue(shop.host.received.isEmpty(), "nothing reached the host");
    }
}
