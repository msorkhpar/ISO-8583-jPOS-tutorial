import com.example.practice.Shapes;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

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
            List<ISOMsg> approved = List.of(
                    purchase("4000000000000002", "000000000875", "001101"),
                    purchase("5105105105105100", "000000012000", "001102"),
                    purchase("4012888888881881", "000000003310", "001103"));
            ISOMsg sameShape = purchase("6011000990139424", "000000000099", "001104");
            System.out.println("same shapes, other values: " + Shapes.unseen(approved, sameShape));
            ISOMsg rejected = purchase("4000000000000002", "0000000O1500", "001105");
            rejected.set(52, ISOUtil.hex2byte("1A2B3C4D5E6F"));
            rejected.unset(49);
            System.out.println("letter in the amount, short PIN block, no currency: " + Shapes.unseen(approved, rejected));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String pan, String amount, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "TERM1502");
        m.set(49, "840");
        m.set(52, ISOUtil.hex2byte("0011223344556677"));
        return m;
    }
}
