import com.example.practice.PanVault;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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
            PanVault vault = new PanVault("key-2026a", key("AES", 17), key("HmacSHA256", 77));
            PanVault.Sealed row = vault.seal("6011000990139424");
            String lookupBefore = vault.lookupValue("6011000990139424");
            vault.rotate("key-2026b", key("AES", 41));
            System.out.println("old row under " + row.keyId() + " opens to: " + vault.open(row));
            PanVault.Sealed moved = vault.rewrap(row);
            vault.retire("key-2026a");
            System.out.println("rewrapped row is under: " + moved.keyId());
            System.out.println("rewrapped row opens to: " + vault.open(moved));
            System.out.println("lookup value unchanged by the rotation: " + lookupBefore.equals(vault.lookupValue("6011000990139424")));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static SecretKey key(String algorithm, int first) {
        byte[] b = new byte[32];
        for (int i = 0; i < b.length; i++)
            b[i] = (byte) (first + i);
        return new SecretKeySpec(b, algorithm);
    }
}
