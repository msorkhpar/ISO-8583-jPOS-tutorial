import com.example.practice.DayTotals;
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
            DayTotals.Totals ours = new DayTotals.Totals(1, 0, 4, 1, 2, 2500, 0, 18750, 4000);
            ISOMsg sent = DayTotals.request(ours, "0415", "000902", "0415231000", "778899");
            System.out.println("0500 DE 97: " + sent.getString(97));
            ISOMsg same = DayTotals.answer(sent, ours);
            System.out.println("same totals: " + same.getMTI() + " DE 66=" + same.getString(66));
            ISOMsg differs = DayTotals.answer(sent, new DayTotals.Totals(1, 0, 3, 1, 2, 2500, 0, 15750, 4000));
            System.out.println("one sale missing: " + differs.getMTI() + " DE 66=" + differs.getString(66));
            ISOMsg refunds = DayTotals.request(new DayTotals.Totals(2, 0, 1, 0, 0, 9000, 0, 1500, 0), "0416", "000903", "0416231000", "778899");
            System.out.println("refund-heavy DE 97: " + refunds.getString(97));
            System.out.println("received 0500 kept: " + sent.getMTI() + ", has DE 66: " + sent.hasField(66));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
