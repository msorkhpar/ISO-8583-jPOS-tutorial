import com.example.practice.NetworkAnswers;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.security.SMAdapter;
import org.jpos.security.SimpleKeyFile;
import org.jpos.security.jceadapter.JCESecurityModule;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;

import java.io.File;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main and see what prints. Submit grades. */
public class TryIt {
    /** Held here, so the JDK's own exit notice stays off: a logger nothing holds can be dropped. */
    private static final Logger JDK_EXIT = Logger.getLogger("java.lang.Runtime");

    static final String TAK = "4C4C4C4C4C4C4C4C9B9B9B9B9B9B9B9B", ZAK = "31313131313131317070707070707070";

    public static void main(String[] args) throws Exception {
        // Turn the logger up, so the log lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        File keyFile = File.createTempFile("tryit", ".keys");
        try {
            JCESecurityModule server = module();
            SimpleKeyFile keys = new SimpleKeyFile(keyFile.getPath());
            keys.setKey("tak.60400092", server.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_TAK, TAK));
            keys.setKey("zak.network", server.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_ZAK, ZAK));
            NameRegistrar.register("hsm", server);
            NameRegistrar.register("ks", keys);
            JCESecurityModule outside = module();
            System.out.println("network approved, signed: " + run(outside, "00", true, false));
            System.out.println("network declined, signed: " + run(outside, "51", true, false));
            System.out.println("decline changed to 00 on the way: " + run(outside, "51", true, true));
            System.out.println("network approved, unsigned: " + run(outside, "00", false, false));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            NameRegistrar.unregister("hsm");
            NameRegistrar.unregister("ks");
            keyFile.delete();
        }
    }

    /** One answer through CheckNetworkMac, the 91 stand-in and SignForTerminal, as the chain runs them. */
    private static String run(JCESecurityModule outside, String rc, boolean signed, boolean changed) throws Exception {
        ISOMsg request = new ISOMsg("0200");
        request.set(2, "4000020000000000");
        request.set(3, "000000");
        request.set(4, "000000002315");
        request.set(7, "1114123000");
        request.set(11, "000917");
        request.set(41, "60400092");
        request.set(49, "978");
        ISOMsg answer = (ISOMsg) request.clone(0, 2, 3, 4, 7, 11, 41, 49);
        answer.setResponseMTI();
        answer.set(39, rc);
        if (signed) answer.set(64, macWith(outside, ZAK, answer));
        if (changed) answer.set(39, "00");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), request);
        ctx.put(ContextConstants.RESPONSE.toString(), answer);
        boolean ok = (new NetworkAnswers.CheckNetworkMac().prepare(1L, ctx) & TransactionConstants.PREPARED) != 0;
        if (ctx.get(ContextConstants.RESPONSE.toString()) == null) {
            ISOMsg failure = (ISOMsg) request.clone(0, 2, 3, 4, 7, 11, 41, 49);
            failure.setResponseMTI();
            failure.set(39, "91");
            ctx.put(ContextConstants.RESPONSE.toString(), failure);
        }
        TransactionParticipant sign = new NetworkAnswers.SignForTerminal();
        if (ok) sign.prepare(1L, ctx);
        else if (sign instanceof AbortParticipant) ((AbortParticipant) sign).prepareForAbort(1L, ctx);
        ISOMsg out = ctx.get(ContextConstants.RESPONSE.toString());
        byte[] mac = out.getBytes(64);
        boolean verifies = mac != null && MessageDigest.isEqual(mac, macWith(outside, TAK, out));
        return "field 39=" + out.getString(39) + ", terminal's MAC check " + (verifies ? "passes" : "fails");
    }

    private static byte[] macWith(JCESecurityModule m, String clear, ISOMsg msg) throws Exception {
        ISOMsg copy = (ISOMsg) msg.clone();
        copy.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        copy.set(64, new byte[8]);
        byte[] packed = copy.pack();
        return m.generateCBC_MAC(Arrays.copyOf(packed, packed.length - 16),
                m.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_TAK, clear));
    }

    private static JCESecurityModule module() throws Exception {
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("provider", "org.bouncycastle.jce.provider.BouncyCastleProvider");
        cfg.put("rebuildlmk", "true");
        JCESecurityModule m = new JCESecurityModule();
        m.setConfiguration(cfg);
        return m;
    }
}
