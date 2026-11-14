import com.example.practice.StandInLimit;
import org.jpos.core.ConfigurationException;
import org.jpos.core.SimpleConfiguration;

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
            StandInLimit gbp = deploy("4000", "826");
            System.out.println("826 limit 4000, 40.00 GBP allowed: " + (gbp != null && gbp.allows("826", 4000)));
            System.out.println("826 limit 4000, 40.01 GBP allowed: " + (gbp != null && gbp.allows("826", 4001)));
            StandInLimit usd = deploy("1500", "${STANDIN_CURRENCY}");
            System.out.println("currency unset, 15.00 USD allowed: " + (usd != null && usd.allows("840", 1500)));
            deploy("${STANDIN_MAX}", "826");
            deploy("40.00", "826");
            deploy(" 4000x", "826");
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    /** What Q2 does with the descriptor's properties; null when the QBean refuses them. */
    private static StandInLimit deploy(String maxAmount, String currency) {
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("max-amount", maxAmount);
        cfg.put("currency", currency);
        StandInLimit limit = new StandInLimit();
        try {
            limit.setConfiguration(cfg);
            System.out.println("max-amount '" + maxAmount + "': accepted");
            return limit;
        } catch (ConfigurationException e) {
            System.out.println("max-amount '" + maxAmount + "': refused, " + e.getMessage());
            return null;
        }
    }
}
