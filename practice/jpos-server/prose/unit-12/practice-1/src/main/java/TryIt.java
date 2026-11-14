import com.example.practice.TerminalLock;
import org.jpos.space.TSpace;

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
            TSpace<String, Object> sp = new TSpace<>();
            TerminalLock purchases = new TerminalLock(sp, 300);
            TerminalLock refunds = new TerminalLock(sp, 300);
            System.out.println("purchases claims PRK00081: " + purchases.claim("PRK00081"));
            System.out.println("refunds claims PRK00081: " + refunds.claim("PRK00081"));
            System.out.println("refunds claims PRK00082: " + refunds.claim("PRK00082"));
            purchases.release("PRK00081");
            System.out.println("after release, refunds claims PRK00081: " + refunds.claim("PRK00081"));
            Thread.sleep(400);
            System.out.println("never released, 400 ms later purchases claims PRK00082: " + purchases.claim("PRK00082"));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
