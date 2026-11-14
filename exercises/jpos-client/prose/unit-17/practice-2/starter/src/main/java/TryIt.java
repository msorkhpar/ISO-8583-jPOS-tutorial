import com.example.practice.ReplayCheck;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
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
            ReplayCheck check = new ReplayCheck(Set.of("7", "11", "37"));
            ISOMsg recorded = response("000301", "731400000301");
            ISOMsg replayed = response("000977", "731500000977");
            replayed.set(7, "1115083000");
            System.out.println("only run-to-run fields changed: " + check.differences(recorded, replayed));
            replayed.set(39, "05");
            replayed.unset(38);
            replayed.set("48.1", "NONE");
            replayed.set(55, ISOUtil.hex2byte("8A023035"));
            System.out.println("declined replay: " + check.differences(recorded, replayed));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg response(String stan, String rrn) throws Exception {
        ISOMsg m = new ISOMsg("0210");
        m.set(3, "000000");
        m.set(4, "000000004410");
        m.set(7, "1114160000");
        m.set(11, stan);
        m.set(37, rrn);
        m.set(38, "Z9Y8X7");
        m.set(39, "00");
        m.set(41, "27180030");
        m.set("48.1", "PROMO");
        m.set(55, ISOUtil.hex2byte("8A023030"));
        return m;
    }
}
