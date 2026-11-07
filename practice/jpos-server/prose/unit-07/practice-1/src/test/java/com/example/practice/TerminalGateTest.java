package com.example.practice;

import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.q2.Q2;
import org.jpos.security.EncryptedPIN;
import org.jpos.security.SMAdapter;
import org.jpos.security.SecureDESKey;
import org.jpos.security.SimpleKeyFile;
import org.jpos.security.jceadapter.JCESecurityModule;
import org.jpos.space.LocalSpace;
import org.jpos.space.SpaceFactory;
import org.jpos.space.SpaceListener;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs TerminalGate first in a TransactionManager deployed by Q2 in this JVM, ahead of a real QueryHost and QMUX. */
class TerminalGateTest {
    // Clear test keys. The server holds them under its module's LMK; the terminals and the network hold their own copies.
    static final String TAK_05 = "3D3D3D3D3D3D3D3D6E6E6E6E6E6E6E6E";
    static final String TPK_05 = "0123456789ABCDEFFEDCBA9876543210";
    static final String TAK_06 = "5B5B5B5B5B5B5B5B2C2C2C2C2C2C2C2C";
    static final String TPK_06 = "13131313131313134A4A4A4A4A4A4A4A";   // the terminal has it; the server's file doesn't yet
    static final String ZPK = "89ABCDEF0123456776543210FEDCBA98";

    @BeforeEach
    void forgetWhatTheNetworkSaw() {
        network.received.clear();
    }

    @Test
    void aGenuinePurchaseIsApproved() throws Exception {
        ISOMsg r = send(terminal05.purchase("000000003175", null));
        assertEquals("00", r.getString(39), "a purchase the terminal signed reaches the network and comes back approved");
        assertEquals(1, network.received.size());
    }

    @Test
    void aForgedMacReachesNobody() throws Exception {
        ISOMsg forged = terminal05.purchase("000000088000", null);
        forged.set(4, "000000099000");                              // changed after signing
        ISOMsg r = send(forged);
        assertEquals("63", r.getString(39));
        assertTrue(network.received.isEmpty(), "the network must never see a request whose MAC doesn't verify");
    }

    @Test
    void aRequestWithoutAMacReachesNobody() throws Exception {
        ISOMsg unsigned = terminal05.purchase("000000088000", null);
        unsigned.unset(64);
        ISOMsg r = send(unsigned);
        assertEquals("63", r.getString(39));
        assertTrue(network.received.isEmpty(), "a request with no MAC is not a genuine one");
    }

    @Test
    void aTerminalNobodyKnowsIsAnswered() throws Exception {
        ISOMsg r = send(stranger.purchase("000000001250", null));
        assertNotNull(r, "a terminal with no key on file still gets an answer");
        assertEquals("63", r.getString(39));
        assertTrue(network.received.isEmpty());
    }

    @Test
    void theTerminalsMacStaysOnItsLink() throws Exception {
        ISOMsg r = send(terminal05.purchase("000000004410", null));
        assertEquals("00", r.getString(39));
        assertEquals(1, network.received.size());
        assertTrue(!network.received.get(0).hasField(64),
                "the network holds no key of this terminal's, so a MAC under one only makes it refuse the message");
    }

    @Test
    void aPinPurchaseIsApproved() throws Exception {
        ISOMsg r = send(terminal05.purchase("000000002760", "8642"));
        assertEquals("00", r.getString(39), "the network can read the PIN, and it is the right one");
        ISOMsg wrong = send(terminal05.purchase("000000002760", "1357"));
        assertEquals("55", wrong.getString(39), "a wrong PIN is still the network's decision");
    }

    @Test
    void aPinWithNoKeyOnFileGoesNowhere() throws Exception {
        ISOMsg r = send(terminal06.purchase("000000002760", "8642"));
        assertEquals("96", r.getString(39));
        assertTrue(network.received.isEmpty(), "a purchase whose PIN can't be passed on must not reach the network at all");
    }

    // ---- the terminals ----

    /** A terminal with its own module: PIN under its TPK, MAC under its TAK. */
    static final class Terminal {
        final String id;
        final JCESecurityModule hsm;
        final SecureDESKey tpk, tak;
        int stan;

        Terminal(String id, String tpkClear, String takClear, int firstStan) throws Exception {
            this.id = id;
            this.hsm = module();
            this.tpk = hsm.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_TPK, tpkClear);
            this.tak = hsm.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_TAK, takClear);
            this.stan = firstStan;
        }

        ISOMsg purchase(String amount, String pin) throws Exception {
            String pan = "5555555555554444";
            ISOMsg m = new ISOMsg("0200");
            m.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
            m.set(2, pan);
            m.set(3, "000000");
            m.set(4, amount);
            m.set(7, "1106153012");
            m.set(11, String.format("%06d", ++stan));
            m.set(41, id);
            m.set(49, "978");
            if (pin != null)
                m.set(52, hsm.exportPIN(hsm.encryptPIN(pin, pan), tpk, SMAdapter.FORMAT01).getPINBlock());
            m.set(64, hsm.generateCBC_MAC(TerminalGate.macInput(m), tak));
            return m;
        }
    }

    // ---- test support: a module and key file, Q2 with a QMUX and a TransactionManager, and a network ----

    private static Q2 q2;
    private static Network network;
    private static Terminal terminal05, terminal06, stranger;

    static JCESecurityModule module() throws Exception {
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("provider", "org.bouncycastle.jce.provider.BouncyCastleProvider");
        cfg.put("rebuildlmk", "true");                              // an LMK in memory only
        JCESecurityModule m = new JCESecurityModule();
        m.setConfiguration(cfg);
        return m;
    }

    static SecureDESKey key(JCESecurityModule m, String type, String clear) throws Exception {
        return m.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, type, clear);
    }

    @BeforeAll
    static void start() throws Exception {
        JCESecurityModule hsm = module();
        Path dir = Files.createTempDirectory("gate");
        SimpleKeyFile keys = new SimpleKeyFile(dir.resolve("keys").toString());
        keys.setKey("tak.29110005", key(hsm, SMAdapter.TYPE_TAK, TAK_05));
        keys.setKey("tpk.29110005", key(hsm, SMAdapter.TYPE_TPK, TPK_05));
        keys.setKey("tak.29110006", key(hsm, SMAdapter.TYPE_TAK, TAK_06));
        keys.setKey("zpk.network", key(hsm, SMAdapter.TYPE_ZPK, ZPK));
        NameRegistrar.register("hsm", hsm);
        NameRegistrar.register("ks", keys);

        terminal05 = new Terminal("29110005", TPK_05, TAK_05, 400);
        terminal06 = new Terminal("29110006", TPK_06, TAK_06, 500);
        stranger = new Terminal("29119999", TPK_05, "7F7F7F7F7F7F7F7F1010101010101010", 600);

        Path deploy = Files.createDirectories(dir.resolve("deploy"));
        Files.writeString(deploy.resolve("20_mux.xml"),
            "<mux class='org.jpos.q2.iso.QMUX' name='network'>\n"
          + "  <in>network-receive</in>\n  <out>network-send</out>\n  <unhandled>network-unhandled</unhandled>\n"
          + "</mux>\n");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-gate' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='practice.gate'/>\n"
          + "  <property name='sessions' value='2'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.TerminalGate'>\n"
          + "    <property name='network-key' value='zpk.network'/>\n"
          + "  </participant>\n"
          + "  <participant class='org.jpos.transaction.participant.QueryHost'>\n"
          + "    <property name='request' value='UPSTREAM'/>\n"
          + "    <property name='timeout' value='2000'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.TerminalGateTest$AnswerFailures'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-gate", 15000), "the TransactionManager did not start");
        assertNotNull(NameRegistrar.get("mux.network", 15000), "the QMUX did not start");
        network = new Network((LocalSpace<String, Object>) SpaceFactory.getSpace());
    }

    @AfterAll
    static void stop() {
        if (q2 != null) q2.shutdown(true);
        NameRegistrar.unregister("hsm");
        NameRegistrar.unregister("ks");
    }

    /** Hands the request to the TransactionManager, as IncomingListener does, and returns the answer. */
    static ISOMsg send(ISOMsg request) throws Exception {
        BlockingQueue<ISOMsg> answers = new LinkedBlockingQueue<>();
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), request);
        ctx.put(ContextConstants.DESTINATION.toString(), "network");
        ctx.put(ContextConstants.SOURCE.toString(), new ISOSource() {
            @Override public void send(ISOMsg m) { answers.add(m); }
            @Override public boolean isConnected() { return true; }
        });
        SpaceFactory.getSpace().out("practice.gate", ctx);
        ISOMsg r = answers.poll(10, TimeUnit.SECONDS);
        assertNotNull(r, "the terminal got no answer");
        return r;
    }

    /** The network: reads the PIN under the zone key (00 or 55), approves the rest. */
    static final class Network implements SpaceListener<String, Object> {
        private static final Map<String, String> PINS = Map.of("5555555555554444", "8642");
        private final LocalSpace<String, Object> sp;
        private final JCESecurityModule hsm;
        private final SecureDESKey zpk;
        final List<ISOMsg> received = java.util.Collections.synchronizedList(new ArrayList<>());

        Network(LocalSpace<String, Object> sp) throws Exception {
            this.sp = sp;
            this.hsm = module();
            this.zpk = key(hsm, SMAdapter.TYPE_ZPK, ZPK);
            sp.addListener("network-send", this);
        }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            received.add(request);
            try {
                ISOMsg response = (ISOMsg) request.clone(0, 2, 3, 4, 7, 11, 41, 49);
                response.setResponseMTI();
                response.set(39, decide(request));
                sp.out("network-receive", response);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private String decide(ISOMsg m) throws Exception {
            if (!m.hasField(52))
                return "00";
            try {
                EncryptedPIN block = new EncryptedPIN(m.getBytes(52), SMAdapter.FORMAT01, m.getString(2));
                String pin = hsm.decryptPIN(hsm.importPIN(block, zpk)); // a test network may; a real one compares in its HSM
                return pin.equals(PINS.get(m.getString(2))) ? "00" : "55";
            } catch (Exception e) {
                return "55";
            }
        }
    }

    /** On the abort path, answers a failure nobody answered: 91 when the network didn't. */
    public static class AnswerFailures implements AbortParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return PREPARED | NO_JOIN | READONLY;
        }

        @Override
        public int prepareForAbort(long id, Serializable context) {
            Context ctx = (Context) context;
            if (ctx.get(ContextConstants.RESPONSE.toString()) == null) {
                try {
                    ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
                    ISOMsg r = (ISOMsg) request.clone(0, 2, 3, 4, 7, 11, 41, 49);
                    r.setResponseMTI();
                    r.set(39, "91");
                    ctx.put(ContextConstants.RESPONSE.toString(), r);
                } catch (Exception e) {
                    ctx.log(e);
                }
            }
            return ABORTED | NO_JOIN;
        }
    }
}
