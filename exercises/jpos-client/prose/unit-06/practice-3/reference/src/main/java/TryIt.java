import com.example.practice.HostSimulator;
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
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        try {
            HostSimulator host = new HostSimulator();
            ISOMsg purchase = request("0200", "040501");
            ISOMsg approved = host.respond(purchase, "00");
            System.out.println("purchase answer: " + approved.getMTI() + " rc=" + approved.getString(39) + " approval=" + approved.getString(38));
            System.out.println("echoes STAN/RRN: " + approved.getString(11) + " / " + approved.getString(37));
            System.out.println("carries field 22 or 52: " + (approved.hasField(22) || approved.hasField(52)));
            System.out.println("advice repeat answer: " + host.respond(request("0221", "040502"), "00").getMTI());
            System.out.println("reversal repeat answer: " + host.respond(request("0421", "040503"), "05").getMTI());
            System.out.println("kept request: " + purchase.getMTI() + ", has field 39: " + purchase.hasField(39));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg request(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "4000002500003155");
        m.set(3, "000000");
        m.set(4, "000000002210");
        m.set(11, stan);
        m.set(22, "051");
        m.set(37, "702201" + stan);
        m.set(41, "40330005");
        m.set(52, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        m.set(49, "826");
        return m;
    }
}
