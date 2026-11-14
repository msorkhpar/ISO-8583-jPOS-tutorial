import com.example.practice.AtmRequests;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
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
            Clock lisbon = Clock.fixed(Instant.parse("2027-06-30T23:58:07Z"), ZoneId.of("Europe/Lisbon"));
            AtomicLong counter = new AtomicLong(999_997);
            AtmRequests atm = new AtmRequests("ATM09120", lisbon, counter::incrementAndGet);
            ISOMsg signOn = atm.signOn();
            System.out.println("sign-on: " + signOn.getMTI() + " field 7=" + signOn.getString(7) + " STAN=" + signOn.getString(11));
            ISOMsg inquiry = atm.balanceInquiry("4012888888881881");
            System.out.println("inquiry: " + inquiry.getMTI() + " STAN=" + inquiry.getString(11));
            ISOMsg cash = atm.withdrawal("4012888888881881", 8000, "978");
            System.out.println("withdrawal: " + cash.getMTI() + " STAN=" + cash.getString(11) + " amount=" + cash.getString(4));
            System.out.println("withdrawal field 7: " + cash.getString(7));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
