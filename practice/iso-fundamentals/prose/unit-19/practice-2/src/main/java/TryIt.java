import com.example.practice.DayTotals;
import java.util.List;
import org.jpos.iso.ISOMsg;
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
            DayTotals.Totals t = DayTotals.count(List.of(
                    entry("0200", "000000", 4250, "00"), entry("0200", "000000", 1999, "00"),
                    entry("0200", "200000", 650, "00"), entry("0200", "000000", 9000, "51"),
                    entry("0100", "000000", 12000, "00")), "0415");
            System.out.println("debits: " + t.debits() + " for " + t.debitsAmount());
            System.out.println("credits: " + t.credits() + " for " + t.creditsAmount());
            System.out.println("authorizations: " + t.authorizations());
            System.out.println("reversals: " + t.debitReversals() + " debit, " + t.creditReversals() + " credit");
            System.out.println("net: " + t.net());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg entry(String mti, String pcode, long amount, String rc) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(3, pcode);
        m.set(4, String.format("%012d", amount));
        m.set(39, rc);
        m.set(15, "0415");
        return m;
    }
}
