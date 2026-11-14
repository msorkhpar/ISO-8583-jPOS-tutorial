import com.example.practice.RefundAmounts;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOMsg;

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
            RefundAmounts portal = new RefundAmounts();
            ISOMsg yen = purchase("000000004800", "392");
            ISOMsg dinar = purchase("000000015250", "414");
            ISOMsg euro = purchase("000000008990", "978");
            System.out.println("yen purchase shown: " + portal.shown(yen));
            System.out.println("Kuwaiti dinar purchase shown: " + portal.shown(dinar));
            System.out.println("yen refund of 1500, field 4: " + portal.refund(yen, "1500").getString(4));
            System.out.println("dinar refund of 3.75, field 4: " + portal.refund(dinar, "3.75").getString(4));
            System.out.println("euro refund of 12.5, field 4: " + portal.refund(euro, "12.5").getString(4));
            try {
                portal.refund(yen, "99.9");
            } catch (IllegalArgumentException refused) {
                System.out.println("yen refund of 99.9 refused: " + refused.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String amount, String currency) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4000000000000002");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(41, "TERM1100");
        m.set(49, currency);
        return m;
    }
}
