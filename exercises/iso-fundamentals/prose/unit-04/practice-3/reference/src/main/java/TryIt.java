import com.example.practice.Journal;
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
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        try {
            Journal journal = new Journal();
            ISOMsg monday = request("000555", "1109083000", "4567890");
            ISOMsg tuesday = request("000555", "1110083000", "4567890");
            journal.record(monday);
            journal.record(tuesday);

            ISOMsg reversal = new ISOMsg("0420");
            reversal.set(11, "000812");
            reversal.set(90, "0200" + "000555" + "1110083000" + "00004567890" + "00000000000");
            ISOMsg ack = journal.onReversal(reversal);
            System.out.println("ack: " + ack.getMTI() + " stan " + ack.getString(11) + " 39=" + ack.getString(39));
            System.out.println("tuesday reversed: " + journal.isReversed(tuesday));
            System.out.println("monday reversed: " + journal.isReversed(monday));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg request(String stan, String when, String acquirer) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(4, "000000001800");
        m.set(7, when);
        m.set(11, stan);
        m.set(32, acquirer);
        m.set(41, "TERM0300");
        return m;
    }
}
