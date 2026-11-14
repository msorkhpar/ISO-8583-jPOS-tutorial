import com.example.practice.PaymentsEndpoint;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
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
            AtomicInteger sent = new AtomicInteger();
            PaymentsEndpoint.Host host = m -> {
                sent.incrementAndGet();
                if (m.getString(4).equals("000000009900"))
                    return null;
                ISOMsg r = (ISOMsg) m.clone();
                r.setResponseMTI();
                r.set(39, "00");
                r.set(38, "A" + m.getString(11).substring(1));
                return r;
            };
            PaymentsEndpoint endpoint = new PaymentsEndpoint(host, new PaymentsEndpoint.KeyTable());
            System.out.println("first call: " + endpoint.pay("cafe-17", payment("6.25")));
            System.out.println("retry, same key: " + endpoint.pay("cafe-17", payment("6.25")));
            System.out.println("same key, other amount: " + endpoint.pay("cafe-17", payment("7.00")));
            System.out.println("no answer: " + endpoint.pay("cafe-18", payment("99.00")));
            System.out.println("retry after no answer: " + endpoint.pay("cafe-18", payment("99.00")));
            System.out.println("0200s sent to the host: " + sent.get() + ", reversals queued: " + endpoint.reversals().size());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static PaymentsEndpoint.Payment payment(String amount) {
        return new PaymentsEndpoint.Payment("4000000000000077", new BigDecimal(amount), "TERM1202");
    }
}
