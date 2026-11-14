import com.example.practice.ScriptedMux;
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
            ScriptedMux host = new ScriptedMux().then("00", "51", ScriptedMux.SILENT);
            ISOMsg auth = message("0100", "000301");
            ISOMsg first = host.request(auth, 1000);
            System.out.println("first: " + first.getMTI() + " 39=" + first.getString(39));
            ISOMsg second = host.request(message("0100", "000302"), 1000);
            System.out.println("second: " + second.getMTI() + " 39=" + second.getString(39));
            System.out.println("third (silent): " + host.request(message("0100", "000303"), 1000));
            ISOMsg reversal = host.request(message("0400", "000304"), 1000);
            System.out.println("after script: " + reversal.getMTI() + " 39=" + reversal.getString(39));
            System.out.println("kept request MTI: " + auth.getMTI());
            System.out.println("sent: " + host.sent().size());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg message(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "4111111111111111");
        m.set(3, "000000");
        m.set(4, "000000001999");
        m.set(11, stan);
        m.set(41, "31415926");
        return m;
    }
}
