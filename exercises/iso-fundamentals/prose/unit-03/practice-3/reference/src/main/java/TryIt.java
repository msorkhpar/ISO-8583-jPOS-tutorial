import com.example.practice.TlvReader;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOUtil;

/** Run executes this file. Change the calls in main and see what prints. Submit grades. */
public class TryIt {
    /** Held here, so the JDK's own exit notice stays off: a logger nothing holds can be dropped. */
    private static final Logger JDK_EXIT = Logger.getLogger("java.lang.Runtime");

    public static void main(String[] args) {
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
            String dump = "9F0206000000012345" + "5F2A020840" + "9A03261114" + "9C0109" + "DF81020101";
            for (TlvReader.Tlv tlv : TlvReader.read(ISOUtil.hex2byte(dump))) {
                System.out.println("tag " + tlv.tag() + " value " + tlv.value());
            }
            try {
                TlvReader.read(ISOUtil.hex2byte("9F02060000"));
            } catch (IllegalArgumentException refused) {
                System.out.println("damaged dump refused: " + refused.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
