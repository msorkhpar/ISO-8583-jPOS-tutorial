import com.example.practice.IssuerLimit;
import java.util.Map;
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
            IssuerLimit limit = new IssuerLimit(Map.of("bank-a", 1, "bank-b", 1));
            ISOMsg first = limit.forward(purchase("110001"), "bank-a", request -> {
                ISOMsg second = limit.forward(purchase("110002"), "bank-a", TryIt::approve);
                System.out.println("bank-a full, stan 110002: " + second.getMTI() + " 39=" + second.getString(39));
                ISOMsg other = limit.forward(purchase("110003"), "bank-b", TryIt::approve);
                System.out.println("bank-b meanwhile, stan 110003: " + other.getMTI() + " 39=" + other.getString(39));
                return approve(request);
            });
            System.out.println("bank-a, stan 110001: " + first.getMTI() + " 39=" + first.getString(39));
            ISOMsg later = limit.forward(purchase("110004"), "bank-a", TryIt::approve);
            System.out.println("bank-a afterwards, stan 110004: " + later.getMTI() + " 39=" + later.getString(39));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(2, "4000000000000010");
        m.set(4, "000000006200");
        m.set(11, stan);
        m.set(41, "TERM1130");
        return m;
    }

    private static ISOMsg approve(ISOMsg request) throws Exception {
        ISOMsg answer = (ISOMsg) request.clone();
        answer.setResponseMTI();
        answer.set(39, "00");
        return answer;
    }
}
