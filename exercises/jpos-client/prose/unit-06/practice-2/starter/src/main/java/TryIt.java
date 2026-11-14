import com.example.practice.Loyalty;
import java.nio.charset.StandardCharsets;
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
            ISOMsg redeem = Loyalty.purchase("4000001240000000", 7250, "030301", "PX", "889100231", 1200);
            redeem.setPackager(Loyalty.packager());
            byte[] wire = redeem.pack();
            System.out.println("on the wire: " + new String(wire, StandardCharsets.ISO_8859_1));
            ISOMsg atHost = new ISOMsg();
            atHost.setPackager(Loyalty.packager());
            atHost.unpack(wire);
            System.out.println("programme: " + atHost.getString("63.1"));
            System.out.println("member: " + atHost.getString("63.2"));
            System.out.println("points to redeem: " + atHost.getString("63.3"));
            ISOMsg plain = Loyalty.purchase("4000001240000000", 990, "030302", null, null, 0);
            System.out.println("non-member has field 63: " + plain.hasField(63));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
