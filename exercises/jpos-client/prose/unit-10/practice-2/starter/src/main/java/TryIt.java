import com.example.practice.HostLog;
import org.jpos.iso.ISOMsg;
import org.jpos.space.Space;
import org.jpos.space.TSpace;
import java.util.concurrent.TimeoutException;
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
            Space<String, Object> sp = new TSpace<>();
            HostLog log = new HostLog(sp, "host.received");
            sp.out("host.received", message("0800", "000501"));
            Thread host = new Thread(() -> {
                try {
                    Thread.sleep(200);
                    sp.out("host.received", message("0420", "000502"));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            host.start();
            long t0 = System.currentTimeMillis();
            ISOMsg got = log.awaitReceived("0420", 3000);
            System.out.println("got: " + got.getMTI() + " stan=" + got.getString(11));
            System.out.println("came before the 3 s timeout: " + (System.currentTimeMillis() - t0 < 2000));
            host.join();
            try {
                log.awaitReceived("0420", 300);
                System.out.println("second wait: returned");
            } catch (TimeoutException e) {
                System.out.println("second wait: " + e.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg message(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(11, stan);
        m.set(41, "31415926");
        return m;
    }
}
