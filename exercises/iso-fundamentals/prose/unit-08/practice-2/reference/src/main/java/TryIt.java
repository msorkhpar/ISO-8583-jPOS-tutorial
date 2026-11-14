import com.example.practice.NetworkMac;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
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
            byte[] zak = ISOUtil.hex2byte("1032547698BADCFE0123456789ABCDEF");
            byte[] answer = "0210302000000280000000000000000560000412TERM0800".getBytes(StandardCharsets.US_ASCII);
            byte[] mac = NetworkMac.mac(zak, answer);
            System.out.println("MAC of the 0210: " + ISOUtil.hexString(mac));
            System.out.println("right MAC accepted: " + NetworkMac.verify(zak, answer, mac.clone()));
            byte[] tampered = mac.clone();
            tampered[0] ^= 0x10;
            System.out.println("changed MAC accepted: " + NetworkMac.verify(zak, answer, tampered));
            System.out.println("shortened MAC accepted: " + NetworkMac.verify(zak, answer, Arrays.copyOf(mac, 4)));
            System.out.println("missing MAC accepted: " + NetworkMac.verify(zak, answer, null));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
