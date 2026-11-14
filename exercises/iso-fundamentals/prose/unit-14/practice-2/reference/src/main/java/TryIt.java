import com.example.practice.TokenVault;
import java.util.Random;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOException;

/** Run executes this file. Change the calls in main and see what prints. Submit grades. */
public class TryIt {
    /** Held here, so the JDK's own exit notice stays off: a logger nothing holds can be dropped. */
    private static final Logger JDK_EXIT = Logger.getLogger("java.lang.Runtime");

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
        try {
            TokenVault vault = new TokenVault("9123450", new Random(77), new TokenVault.Sealer() {
                public String seal(String pan) { return "sealed:" + new StringBuilder(pan).reverse(); }
                public String open(String sealed) { return new StringBuilder(sealed.substring(7)).reverse().toString(); }
                public String lookup(String pan) { return "lookup:" + Integer.toHexString(("k7" + pan).hashCode()); }
            });
            Set<String> station = Set.of("DETOKENIZE");
            String gym = vault.tokenize("gym-2", "4012888888881881");
            System.out.println("station for gym-2: " + ask(vault, station, "gym-2", gym));
            System.out.println("gym app's own credentials: " + ask(vault, Set.of("TOKENIZE"), "gym-2", gym));
            System.out.println("station for florist-5: " + ask(vault, station, "florist-5", gym));
            System.out.println("made-up token: " + ask(vault, station, "gym-2", "9123450000000004"));
            vault.revoke(gym);
            System.out.println("after revoke: " + ask(vault, station, "gym-2", gym));
            System.out.println("new token for the same card: " + !gym.equals(vault.tokenize("gym-2", "4012888888881881")));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static String ask(TokenVault vault, Set<String> permissions, String requestor, String token) {
        try {
            return vault.detokenize(permissions, requestor, token);
        } catch (SecurityException | ISOException refused) {
            return "refused (" + refused.getClass().getSimpleName() + ": " + refused.getMessage() + ")";
        }
    }
}
