import com.example.practice.LateAnswers;
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
            LateAnswers listener = new LateAnswers(r -> System.out.println("ledger: late approval STAN " + r.getString(11)));
            System.out.println("late 0210, rc 00 taken: " + listener.process(null, message("0210", "000611", "00")));
            System.out.println("late 0110, rc 05 taken: " + listener.process(null, message("0110", "000612", "05")));
            System.out.println("late 0810 taken: " + listener.process(null, message("0810", "000613", "00")));
            System.out.println("host's own 0800 taken: " + listener.process(null, message("0800", "500614", null)));
            LateAnswers locked = new LateAnswers(r -> {
                throw new IllegalStateException("ledger table is locked");
            });
            System.out.println("late 0210, ledger locked, taken: " + locked.process(null, message("0210", "000615", "00")));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg message(String mti, String stan, String rc) {
        ISOMsg m = new ISOMsg(mti);
        m.set(3, "000000");
        m.set(4, "000000006600");
        m.set(11, stan);
        m.set(41, "40110019");
        if (rc != null)
            m.set(39, rc);
        return m;
    }
}
