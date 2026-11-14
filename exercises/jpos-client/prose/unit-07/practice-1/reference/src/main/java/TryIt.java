import com.example.practice.PinRelay;
import java.util.Map;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.jpos.security.EncryptedPIN;
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
            JCESecurityModule hsm = module();
            String oldKey = "1122334455667788AABBCCDDEEFF0011", newKey = "99887766554433221100FFEEDDCCBBAA";
            String hostKey = "0123456789ABCDEFFEDCBA9876543210";
            PinRelay relay = new PinRelay(hsm, Map.of(
                    "50010001", new PinRelay.Terminal(key(hsm, SMAdapter.TYPE_TPK, oldKey), SMAdapter.FORMAT01),
                    "50010002", new PinRelay.Terminal(key(hsm, SMAdapter.TYPE_TPK, newKey), SMAdapter.FORMAT05)),
                    key(hsm, SMAdapter.TYPE_ZPK, hostKey), new Host(hsm, key(hsm, SMAdapter.TYPE_ZPK, hostKey)));
            ISOMsg chip = purchase("50010001", "061201");
            chip.set(2, "4111111111111111");
            chip.set(52, pinBlock(hsm, "2468", "4111111111111111", oldKey, SMAdapter.FORMAT01));
            System.out.println("old model, chip: rc " + relay.send(chip, 1000).getString(39));
            ISOMsg swiped = purchase("50010002", "061202");
            swiped.set(35, "5555555555554444=30121010000012345");
            swiped.set(52, pinBlock(hsm, "802211", "5555555555554444", newKey, SMAdapter.FORMAT05));
            System.out.println("new model, swiped: rc " + relay.send(swiped, 1000).getString(39));
            ISOMsg stale = purchase("50010001", "061203");
            stale.set(2, "4111111111111111");
            stale.set(52, pinBlock(hsm, "2468", "4111111111111111", "0000111122223333444455556666777F", SMAdapter.FORMAT01));
            System.out.println("stale key: rc " + relay.send(stale, 1000).getString(39));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
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

    private static byte[] pinBlock(JCESecurityModule m, String pin, String pan, String clearKey, byte format) throws Exception {
        return m.exportPIN(m.encryptPIN(pin, pan), key(m, SMAdapter.TYPE_TPK, clearKey), format).getPINBlock();
    }

    private static ISOMsg purchase(String terminal, String stan) {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, "000000004100");
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    /** The host: reads ISO format 0 under its key and prints the PIN it found. */
    private record Host(JCESecurityModule hsm, SecureDESKey zpk) implements MUX {
        @Override public ISOMsg request(ISOMsg m, long timeout) {
            try {
                String pan = m.hasField(2) ? m.getString(2) : m.getString(35).split("=")[0];
                String pin = hsm.decryptPIN(hsm.importPIN(new EncryptedPIN(m.getBytes(52), SMAdapter.FORMAT01, pan), zpk));
                System.out.println("host read PIN " + pin + " for " + pan);
                ISOMsg r = (ISOMsg) m.clone(3, 4, 11, 41);
                r.setMTI("0210");
                r.set(39, "00");
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
