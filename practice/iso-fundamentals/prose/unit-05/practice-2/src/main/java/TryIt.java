import com.example.practice.DailyReport;
import java.util.List;
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
            List<ISOMsg> day = List.of(
                    msg("0200", "000000", "000000000480"),
                    msg("0200", "003000", "000000000220"),
                    msg("0200", "010000", "000000004000"),
                    msg("0200", "200000", "000000000150"),
                    msg("0420", "000000", "000000000480"),
                    msg("0800", null, null));
            for (ISOMsg m : day) {
                System.out.println(m.getMTI() + " " + m.getString(3) + " is " + DailyReport.kind(m));
            }
            System.out.println("totals: " + DailyReport.totals(day));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg msg(String mti, String processingCode, String amount) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        if (processingCode != null)
            m.set(3, processingCode);
        if (amount != null)
            m.set(4, amount);
        m.set(41, "TERM0600");
        return m;
    }
}
