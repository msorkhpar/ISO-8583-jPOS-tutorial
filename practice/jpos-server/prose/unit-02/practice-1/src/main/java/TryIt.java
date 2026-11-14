import com.example.practice.Responder;
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
            Responder responder = new Responder();
            ISOMsg purchase = cardRequest("0200", "000801");
            System.out.println("approved 0200: " + show(responder.answer(purchase, "00", "K3J9P2")));
            System.out.println("declined 0100: " + show(responder.answer(cardRequest("0100", "000802"), "51", "K3J9P2")));
            System.out.println("0220 advice: " + show(responder.answer(cardRequest("0220", "000803"), "00", "K3J9P3")));
            System.out.println("0421 repeat: " + show(responder.answer(cardRequest("0421", "000804"), "00", "K3J9P4")));
            System.out.println("request after answering: " + purchase.getMTI() + " 39=" + purchase.getString(39));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg cardRequest(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "4000056655665556");
        m.set(3, "000000");
        m.set(4, "000000006210");
        m.set(7, "1114120510");
        m.set(11, stan);
        m.set(35, "4000056655665556=29121010000000000000");
        m.set(37, "731800000801");
        m.set(41, "POS00788");
        m.set(49, "840");
        return m;
    }

    private static String show(ISOMsg m) throws Exception {
        return m.getMTI() + " 11=" + m.getString(11) + " 37=" + m.getString(37) + " 38=" + m.getString(38)
                + " 39=" + m.getString(39) + " track2 " + (m.hasField(35) ? "present" : "absent");
    }
}
