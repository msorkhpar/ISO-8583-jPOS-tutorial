import com.example.practice.TestCardGuard;
import java.util.List;
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
            TestCardGuard guard = new TestCardGuard(List.of("4111111111", "3714496353"));
            send(guard, request(2, "4111111111111111"));
            send(guard, request(35, "371449635398431=30121010000000000"));
            send(guard, request(2, "4242424242424242"));
            send(guard, request(2, "4111111111111112"));
            send(guard, request(35, "5500005555555559D30121010000000000"));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg request(int field, String card) throws Exception {
        ISOMsg m = new ISOMsg(field == 2 ? "0100" : "0200");
        m.set(3, "000000");
        m.set(4, "000000002240");
        m.set(11, "051001");
        m.set(field, card);
        return m;
    }

    private static void send(TestCardGuard guard, ISOMsg request) {
        try {
            guard.check(request);
            System.out.println("sent: " + request.getString(0) + " with field " + (request.hasField(2) ? 2 : 35));
        } catch (TestCardGuard.Refused refused) {
            System.out.println(refused.getMessage());
        }
    }
}
