import com.example.practice.MacLink;
import java.security.MessageDigest;
import java.util.Arrays;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.security.SMAdapter;
import org.jpos.security.SecureDESKey;
import org.jpos.security.jceadapter.JCESecurityModule;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main and see what prints. Submit grades. */
public class TryIt {
    /** Held here, so the JDK's own exit notice stays off: a logger nothing holds can be dropped. */
    private static final Logger JDK_EXIT = Logger.getLogger("java.lang.Runtime");

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
        try {
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("provider", "org.bouncycastle.jce.provider.BouncyCastleProvider");
            cfg.put("rebuildlmk", "true");
            JCESecurityModule hsm = new JCESecurityModule();
            hsm.setConfiguration(cfg);
            SecureDESKey zak = hsm.formKEYfromClearComponents(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_ZAK,
                    "1A2B3C4D5E6F708192A3B4C5D6E7F801");
            ISOPackager packager = new GenericPackager("jar:packager/iso87ascii.xml");
            Host host = new Host(hsm, zak, packager, new boolean[1]);
            MacLink link = new MacLink(host, hsm, zak);
            System.out.println("purchase 89.90: " + link.send(message(packager, "0200", "072201", "000000008990"), 1000));
            System.out.println("purchase 720.00: " + link.send(message(packager, "0200", "072202", "000000072000"), 1000));
            ISOMsg reversal = message(packager, "0420", "072203", "000000008990");
            reversal.set(90, "020007220111141030220000000500100000000000");
            System.out.println("reversal: " + link.send(reversal, 1000));
            host.tamper()[0] = true;
            System.out.println("approval changed on the way back: " + link.send(message(packager, "0200", "072204", "000000001000"), 1000));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg message(ISOPackager packager, String mti, String stan, String amount) {
        ISOMsg m = new ISOMsg(mti);
        m.setPackager(packager);
        m.set(2, "4000056655665556");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "50020003");
        return m;
    }

    /** The host: checks the MAC, declines over 500.00, signs its answer; tamper changes field 4 after. */
    private record Host(JCESecurityModule hsm, SecureDESKey zak, ISOPackager packager, boolean[] tamper) implements MUX {
        int field(ISOMsg m) {
            for (int f = 65; f <= 127; f++)
                if (m.hasField(f)) return 128;
            return 64;
        }

        byte[] mac(ISOMsg m) throws Exception {
            ISOMsg c = (ISOMsg) m.clone();
            c.set(field(m), new byte[8]);
            byte[] p = c.pack();
            return hsm.generateCBC_MAC(Arrays.copyOf(p, p.length - 16), zak);
        }

        @Override public ISOMsg request(ISOMsg m, long timeout) {
            try {
                boolean ok = m.hasField(field(m)) && MessageDigest.isEqual(m.getBytes(field(m)), mac(m));
                System.out.println("host: MAC in field " + field(m) + (ok ? " verifies" : " does not verify"));
                ISOMsg r = (ISOMsg) m.clone(2, 3, 4, 11, 41, 90);
                r.setPackager(packager);
                r.setMTI(m.getMTI());
                r.setResponseMTI();
                r.set(39, !ok ? "63" : Long.parseLong(m.getString(4)) > 50000 ? "51" : "00");
                r.set(field(r), mac(r));
                if (tamper[0]) r.set(4, "000000000001");
                return r;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        @Override public void request(ISOMsg m, long t, ISOResponseListener l, Object h) { throw new UnsupportedOperationException(); }
        @Override public void send(ISOMsg m) { throw new UnsupportedOperationException(); }
        @Override public boolean isConnected() { return true; }
    }
}
