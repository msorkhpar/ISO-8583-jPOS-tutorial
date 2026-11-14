import com.example.practice.Boarding;
import java.util.Properties;
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
            Properties p = new Properties();
            p.setProperty("terminal", "40220031");
            p.setProperty("merchant", "000000001122334");
            p.setProperty("mcc", "5942");
            p.setProperty("currency", "840");
            p.setProperty("name", "LAKESHORE BOOKS AND MUSIC EMPORIUM");
            p.setProperty("city", "SAINT PETERSBURG");
            p.setProperty("country", "US");
            p.setProperty("acquirer.visa", "412300");
            p.setProperty("acquirer.mastercard", "519900");
            Boarding b = new Boarding(p);
            ISOMsg visa = b.purchase("Visa", "4111111111111111", 2599);
            System.out.println("visa field 32: " + visa.getString(32));
            System.out.println("mastercard field 32: " + b.purchase("MASTERCARD", "5555555555554444", 2599).getString(32));
            System.out.println("field 43: [" + visa.getString(43) + "] length " + visa.getString(43).length());
            try {
                b.purchase("discover", "6011000990139424", 2599);
                System.out.println("discover: built");
            } catch (IllegalArgumentException e) {
                System.out.println("discover: refused, " + e.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
