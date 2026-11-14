import com.example.practice.ChipConsistency;
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
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        try {
            String chip = "9F2608A1A2A3A4A5A6A7A8" + "9F0206000000002599" + "5F2A020840" + "9C0100" + "9A03261114";
            ISOMsg agreeing = message(chip, "000000002599", "840", "1114");
            System.out.println("agreeing message: " + ChipConsistency.mismatches(agreeing));
            ISOMsg wrongAmount = message(chip, "000000002600", "840", "1114");
            System.out.println("amount off by a cent: " + ChipConsistency.mismatches(wrongAmount));
            ISOMsg wrongEverything = message(chip, "000000002599", "978", "1411");
            wrongEverything.set(3, "200000");
            System.out.println("other currency, refund, swapped date: " + ChipConsistency.mismatches(wrongEverything));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg message(String de55, String amount, String currency, String date) throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(2, "4000000000000002");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, "180101");
        m.set(13, date);
        m.set(49, currency);
        m.set(55, ISOUtil.hex2byte(de55));
        return m;
    }
}
