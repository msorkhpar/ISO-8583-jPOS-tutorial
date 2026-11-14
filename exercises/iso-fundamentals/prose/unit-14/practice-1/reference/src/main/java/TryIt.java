import com.example.practice.TokenVault;
import java.util.Random;
import java.util.Set;
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
            TokenVault vault = new TokenVault("9123450", new Random(2026), new TokenVault.Sealer() {
                public String seal(String pan) { return "sealed:" + new StringBuilder(pan).reverse(); }
                public String open(String sealed) { return new StringBuilder(sealed.substring(7)).reverse().toString(); }
                public String lookup(String pan) { return "lookup:" + Integer.toHexString(("k7" + pan).hashCode()); }
            });
            String bakery = vault.tokenize("bakery-4", "4012888888881881");
            System.out.println("bakery's token: " + bakery);
            System.out.println("bakery again, same card: " + vault.tokenize("bakery-4", "4012888888881881"));
            System.out.println("taxi's token, same card: " + vault.tokenize("taxi-9", "4012888888881881"));
            System.out.println("bakery's token, other card: " + vault.tokenize("bakery-4", "5105105105105100"));
            System.out.println("bakery's token leads to: " + vault.detokenize(Set.of("DETOKENIZE"), "bakery-4", bakery));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
