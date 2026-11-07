package com.example.practice;

import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.q2.Q2;
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
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs both participants in a TransactionManager deployed by Q2 in this JVM, around a real QueryHost and QMUX. */
class NetworkAnswersTest {
    static final String TAK_07 = "6A6A6A6A6A6A6A6A1F1F1F1F1F1F1F1F";
    static final String TAK_08 = "2E2E2E2E2E2E2E2E7C7C7C7C7C7C7C7C";
    static final String ZAK = "FEDCBA98765432100123456789ABCDEF";

    @Test
    void theNetworksApprovalReachesTheTerminal() throws Exception {
        ISOMsg r = send("29110007", TAK_07, "000000004120", Network.APPROVE);
        assertEquals("00", r.getString(39));
        assertTrue(verifies(r, TAK_07), "the terminal checks every answer under its own key");
        ISOMsg other = send("29110008", TAK_08, "000000000990", Network.APPROVE);
        assertEquals("00", other.getString(39));
        assertTrue(verifies(other, TAK_08), "each terminal checks under its own key");
    }

    @Test
    void theNetworksDeclineReachesTheTerminal() throws Exception {
        ISOMsg r = send("29110007", TAK_07, "000000006300", Network.DECLINE);
        assertEquals("05", r.getString(39));
        assertTrue(verifies(r, TAK_07));
    }

    @Test
    void anAnswerChangedOnTheWayIsNotPassedOn() throws Exception {
        ISOMsg r = send("29110007", TAK_07, "000000007700", Network.DECLINE_CHANGED_TO_APPROVE);
        assertEquals("91", r.getString(39), "an answer that fails the network's MAC says nothing about what the network decided");
        assertTrue(verifies(r, TAK_07));
    }

    @Test
    void anUnsignedAnswerIsNotPassedOn() throws Exception {
        ISOMsg r = send("29110008", TAK_08, "000000002240", Network.APPROVE_UNSIGNED);
        assertEquals("91", r.getString(39), "an answer without the network's MAC is no better than a wrong one");
        assertTrue(verifies(r, TAK_08));
    }

    @Test
    void aDeclineOfOurOwnCanBeChecked() throws Exception {
        ISOMsg r = send("29110007", TAK_07, "000000150000", Network.APPROVE);   // over our 1000.00 limit
        assertEquals("61", r.getString(39));
        assertTrue(verifies(r, TAK_07), "the terminal must be able to tell our decline from a forged one");
    }

    // ---- the terminal's side ----

    static boolean verifies(ISOMsg answer, String takClear) throws Exception {
        byte[] received = answer.getBytes(64);
        return received != null && MessageDigest.isEqual(received, macWith(terminalModule, takClear, answer));
    }

    static byte[] macWith(JCESecurityModule m, String clear, ISOMsg msg) throws Exception {
        ISOMsg copy = (ISOMsg) msg.clone();
        copy.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        copy.set(64, new byte[8]);
        byte[] packed = copy.pack();
        return m.generateCBC_MAC(Arrays.copyOf(packed, packed.length - 16),
                m.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_TAK, clear));
    }

    // ---- test support ----

    private static Q2 q2;
    private static Network network;
    private static JCESecurityModule terminalModule;
    private static int stan = 300;

    static JCESecurityModule module() throws Exception {
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("provider", "org.bouncycastle.jce.provider.BouncyCastleProvider");
        cfg.put("rebuildlmk", "true");
        JCESecurityModule m = new JCESecurityModule();
        m.setConfiguration(cfg);
        return m;
    }

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void start() throws Exception {
        JCESecurityModule hsm = module();
        Path dir = Files.createTempDirectory("answers");
        SimpleKeyFile keys = new SimpleKeyFile(dir.resolve("keys").toString());
        keys.setKey("tak.29110007", hsm.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_TAK, TAK_07));
        keys.setKey("tak.29110008", hsm.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_TAK, TAK_08));
        keys.setKey("zak.network", hsm.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_ZAK, ZAK));
        NameRegistrar.register("hsm", hsm);
        NameRegistrar.register("ks", keys);
        terminalModule = module();

        Path deploy = Files.createDirectories(dir.resolve("deploy"));
        Files.writeString(deploy.resolve("20_mux.xml"),
            "<mux class='org.jpos.q2.iso.QMUX' name='network'>\n"
          + "  <in>answers-receive</in>\n  <out>answers-send</out>\n  <unhandled>answers-unhandled</unhandled>\n"
          + "</mux>\n");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-answers' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='practice.answers'/>\n"
          + "  <property name='sessions' value='2'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.NetworkAnswersTest$Limit'/>\n"
          + "  <participant class='org.jpos.transaction.participant.QueryHost'>\n"
          + "    <property name='timeout' value='2000'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.NetworkAnswers$CheckNetworkMac'/>\n"
          + "  <participant class='com.example.practice.NetworkAnswersTest$AnswerFailures'/>\n"
          + "  <participant class='com.example.practice.NetworkAnswers$SignForTerminal'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-answers", 15000), "the TransactionManager did not start");
        assertNotNull(NameRegistrar.get("mux.network", 15000), "the QMUX did not start");
        network = new Network((LocalSpace<String, Object>) SpaceFactory.getSpace());
    }

    @AfterAll
    static void stop() {
        if (q2 != null) q2.shutdown(true);
        NameRegistrar.unregister("hsm");
        NameRegistrar.unregister("ks");
    }

    /** A signed purchase from the terminal, handed to the TransactionManager as IncomingListener does. */
    static ISOMsg send(String terminal, String takClear, String amount, String networkDoes) throws Exception {
        network.next = networkDoes;
        ISOMsg m = new ISOMsg("0200");
        m.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        m.set(2, "4111111111111111");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(7, "1106164500");
        m.set(11, String.format("%06d", ++stan));
        m.set(41, terminal);
        m.set(49, "978");
        m.set(64, macWith(terminalModule, takClear, m));
        BlockingQueue<ISOMsg> answers = new LinkedBlockingQueue<>();
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        ctx.put(ContextConstants.DESTINATION.toString(), "network");
        ctx.put(ContextConstants.SOURCE.toString(), new ISOSource() {
            @Override public void send(ISOMsg r) { answers.add(r); }
            @Override public boolean isConnected() { return true; }
        });
        SpaceFactory.getSpace().out("practice.answers", ctx);
        ISOMsg r = answers.poll(10, TimeUnit.SECONDS);
        assertNotNull(r, "the terminal got no answer");
        return r;
    }

    /** The network: answers every request, signed with the zone MAC key, unless told otherwise. */
    static final class Network implements SpaceListener<String, Object> {
        static final String APPROVE = "approve", DECLINE = "decline",
                DECLINE_CHANGED_TO_APPROVE = "changed", APPROVE_UNSIGNED = "unsigned";
        private final LocalSpace<String, Object> sp;
        private final JCESecurityModule hsm;
        volatile String next = APPROVE;

        Network(LocalSpace<String, Object> sp) throws Exception {
            this.sp = sp;
            this.hsm = module();
            sp.addListener("answers-send", this);
        }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            try {
                ISOMsg r = (ISOMsg) request.clone(0, 2, 3, 4, 7, 11, 41, 49);
                r.setResponseMTI();
                r.set(39, DECLINE.equals(next) || DECLINE_CHANGED_TO_APPROVE.equals(next) ? "05" : "00");
                if (!APPROVE_UNSIGNED.equals(next))
                    r.set(64, macWith(hsm, ZAK, r));
                if (DECLINE_CHANGED_TO_APPROVE.equals(next))
                    r.set(39, "00");                                // changed after the network signed it
                sp.out("answers-receive", r);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Our own limit: a purchase over 1000.00 is declined 61 before it goes anywhere. */
    public static class Limit implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
            if (Long.parseLong(request.getString(4)) <= 100000)
                return PREPARED | NO_JOIN | READONLY;
            ctx.put(ContextConstants.RESPONSE.toString(), answer(request, "61"));
            return ABORTED | NO_JOIN;
        }
    }

    /** On the abort path, answers 91 when nobody has answered. */
    public static class AnswerFailures implements AbortParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return PREPARED | NO_JOIN | READONLY;
        }

        @Override
        public int prepareForAbort(long id, Serializable context) {
            Context ctx = (Context) context;
            if (ctx.get(ContextConstants.RESPONSE.toString()) == null)
                ctx.put(ContextConstants.RESPONSE.toString(), answer(ctx.get(ContextConstants.REQUEST.toString()), "91"));
            return ABORTED | NO_JOIN;
        }
    }

    static ISOMsg answer(ISOMsg request, String rc) {
        try {
            ISOMsg r = (ISOMsg) request.clone(0, 2, 3, 4, 7, 11, 41, 49);
            r.setResponseMTI();
            r.set(39, rc);
            return r;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
