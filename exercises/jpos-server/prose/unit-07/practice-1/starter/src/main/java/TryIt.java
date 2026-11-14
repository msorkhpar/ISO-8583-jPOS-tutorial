import com.example.practice.TerminalGate;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.security.EncryptedPIN;
import org.jpos.security.SMAdapter;
import org.jpos.security.SecureDESKey;
import org.jpos.security.SimpleKeyFile;
import org.jpos.security.jceadapter.JCESecurityModule;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.util.NameRegistrar;

import java.io.File;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main and see what prints. Submit grades. */
public class TryIt {
    /** Held here, so the JDK's own exit notice stays off: a logger nothing holds can be dropped. */
    private static final Logger JDK_EXIT = Logger.getLogger("java.lang.Runtime");

    static final String TAK = "1F1F1F1F1F1F1F1F7A7A7A7A7A7A7A7A", TPK = "25252525252525256767676767676767";
    static final String ZPK = "A1A1A1A1A1A1A1A1C4C4C4C4C4C4C4C4";

    public static void main(String[] args) throws Exception {
        // Turn the logger up, so the log lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        // Only this handler prints, and it leaves out the JDK's own detail.
        Logger root = Logger.getLogger("");
        for (java.util.logging.Handler before : root.getHandlers()) {
            root.removeHandler(before);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFilter(entry -> !String.valueOf(entry.getLoggerName()).matches("(java|javax|jdk|sun|com\\.sun)\\..*"));
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        File keyFile = File.createTempFile("tryit", ".keys");
        try {
            JCESecurityModule server = module(), terminal = module(), network = module();
            SimpleKeyFile keys = new SimpleKeyFile(keyFile.getPath());
            keys.setKey("tak.60400084", key(server, SMAdapter.TYPE_TAK, TAK));
            keys.setKey("tpk.60400084", key(server, SMAdapter.TYPE_TPK, TPK));
            keys.setKey("zpk.network", key(server, SMAdapter.TYPE_ZPK, ZPK));
            NameRegistrar.register("hsm", server);
            NameRegistrar.register("ks", keys);
            TerminalGate gate = new TerminalGate();
            gate.setConfiguration(new SimpleConfiguration());

            ISOMsg pinPurchase = purchase(terminal, "60400084", "000000001840", "2580");
            Context ctx = run(gate, pinPurchase);
            ISOMsg up = ctx.get(TerminalGate.UPSTREAM);
            System.out.println("PIN purchase forwarded: " + (up != null));
            System.out.println("forwarded with field 64: " + (up != null && up.hasField(64)));
            if (up != null) {
                EncryptedPIN block = new EncryptedPIN(up.getBytes(52), SMAdapter.FORMAT01, up.getString(2));
                System.out.println("PIN the network reads: " + network.decryptPIN(network.importPIN(block, key(network, SMAdapter.TYPE_ZPK, ZPK))));
            }
            ISOMsg changed = purchase(terminal, "60400084", "000000001840", null);
            changed.set(4, "000000091840");
            System.out.println("changed amount: " + answer(run(gate, changed)));
            System.out.println("unknown terminal: " + answer(run(gate, purchase(terminal, "60409999", "000000000500", null))));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            NameRegistrar.unregister("hsm");
            NameRegistrar.unregister("ks");
            keyFile.delete();
        }
    }

    private static Context run(TerminalGate gate, ISOMsg request) {
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), request);
        gate.prepare(1L, ctx);
        return ctx;
    }

    private static String answer(Context ctx) {
        ISOMsg r = ctx.get(ContextConstants.RESPONSE.toString());
        return r == null ? "forwarded" : "refused, field 39=" + r.getString(39);
    }

    private static ISOMsg purchase(JCESecurityModule hsm, String terminal, String amount, String pin) throws Exception {
        String pan = "4761739001010119";
        ISOMsg m = new ISOMsg("0200");
        m.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, amount);
        m.set(7, "1114120000");
        m.set(11, "000851");
        m.set(41, terminal);
        m.set(49, "978");
        if (pin != null)
            m.set(52, hsm.exportPIN(hsm.encryptPIN(pin, pan), key(hsm, SMAdapter.TYPE_TPK, TPK), SMAdapter.FORMAT01).getPINBlock());
        m.set(64, hsm.generateCBC_MAC(macInput(m), key(hsm, SMAdapter.TYPE_TAK, TAK)));
        return m;
    }

    private static byte[] macInput(ISOMsg msg) throws Exception {
        ISOMsg copy = (ISOMsg) msg.clone();
        copy.set(64, new byte[8]);
        byte[] packed = copy.pack();
        int macLength = ((ISOBasePackager) copy.getPackager()).getFieldPackager(64).getMaxPackedLength();
        return Arrays.copyOf(packed, packed.length - macLength);
    }

    private static JCESecurityModule module() throws Exception {
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("provider", "org.bouncycastle.jce.provider.BouncyCastleProvider");
        cfg.put("rebuildlmk", "true");
        JCESecurityModule m = new JCESecurityModule();
        m.setConfiguration(cfg);
        return m;
    }

    private static SecureDESKey key(JCESecurityModule m, String type, String clear) throws Exception {
        return m.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, type, clear);
    }
}
