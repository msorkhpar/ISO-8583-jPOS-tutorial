import com.example.practice.ChargeMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOException;
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
            ChargeMapper mapper = new ChargeMapper();
            ZoneId zurich = ZoneId.of("Europe/Zurich");
            ZoneId kuwait = ZoneId.of("Asia/Kuwait");
            System.out.println("Zurich sale: " + mapper.toCharge(request("000000", "000000012850", "756", "0815", "193000"),
                    Instant.parse("2031-08-15T17:30:20Z"), zurich));
            System.out.println("Kuwait refund: " + mapper.toCharge(request("200000", "000000004125", "414", "0420", "091500"),
                    Instant.parse("2031-04-20T06:15:10Z"), kuwait));
            System.out.println("last year's sale: " + mapper.toCharge(request("000000", "000000000990", "978", "1231", "221000"),
                    Instant.parse("2032-01-02T09:00:00Z"), zurich).localTime());
            try {
                mapper.toCharge(request("010000", "000000005000", "978", "0815", "120000"), Instant.parse("2031-08-15T10:00:30Z"), zurich);
            } catch (ISOException refused) {
                System.out.println("cash withdrawal refused: " + refused.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg request(String de3, String de4, String de49, String de13, String de12) throws ISOException {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, de3);
        m.set(4, de4);
        m.set(11, "120501");
        m.set(12, de12);
        m.set(13, de13);
        m.set(41, "TERM1201");
        m.set(49, de49);
        return m;
    }
}
