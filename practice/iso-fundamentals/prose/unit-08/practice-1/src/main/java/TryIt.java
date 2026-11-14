import com.example.practice.PinBlockReader;
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
            String pan = "4012888888881881";
            String mask = "0000" + pan.substring(3, 15);
            System.out.println("format 0: " + PinBlockReader.pin(xor("042864FFFFFFFFFF", mask), pan));
            System.out.println("format 3: " + PinBlockReader.pin(xor("36902215ACEBDAFB", mask), pan));
            System.out.println("format 1, no card number: " + PinBlockReader.pin("14246813579BDF02", null));
            try {
                PinBlockReader.pin(xor("242864FFFFFFFFFF", mask), pan);
            } catch (IllegalArgumentException refused) {
                System.out.println("format 2 refused: " + refused.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static String xor(String pinField, String mask) {
        return ISOUtil.hexString(ISOUtil.xor(ISOUtil.hex2byte(pinField), ISOUtil.hex2byte(mask)));
    }
}
