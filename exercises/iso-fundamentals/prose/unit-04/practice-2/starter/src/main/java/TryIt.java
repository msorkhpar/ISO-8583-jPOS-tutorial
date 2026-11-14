import com.example.practice.Inbox;
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
            Inbox inbox = new Inbox();
            inbox.expect(msg("0200", "120301", "TERM0101"));
            inbox.expect(msg("0200", "120301", "TERM0102"));
            inbox.expect(msg("0220", "120302", "TERM0101"));
            System.out.println("0210 for TERM0102 reaches: " + who(inbox.deliver(msg("0210", "120301", "TERM0102"))));
            System.out.println("0230 for TERM0101 reaches: " + who(inbox.deliver(msg("0230", "120302", "TERM0101"))));
            System.out.println("0110 for TERM0101 reaches: " + who(inbox.deliver(msg("0110", "120301", "TERM0101"))));
            System.out.println("0210 for TERM0101 reaches: " + who(inbox.deliver(msg("0210", "120301", "TERM0101"))));
            System.out.println("still waiting: " + inbox.waiting());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg msg(String mti, String stan, String terminal) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    private static String who(ISOMsg sent) {
        return sent == null ? "nobody" : sent.getString(0) + " stan " + sent.getString(11) + " terminal " + sent.getString(41);
    }
}
