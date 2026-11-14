import com.example.practice.MastercardLayout;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;

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
            GenericPackager packager = MastercardLayout.packager();
            ISOMsg sent = new ISOMsg("0200");
            sent.set(2, "5200828282828210");
            sent.set(3, "000000");
            sent.set(4, "000000004875");
            sent.set(11, "026114");
            sent.set(41, "KIOSK7");
            sent.set(49, "840");
            sent.setPackager(packager);
            String hex = ISOUtil.hexString(sent.pack());
            System.out.println("mti and bitmap: " + hex.substring(0, 24));
            System.out.println("field 2 on the wire: " + hex.substring(24, 60));
            System.out.println("whole message: " + hex);

            ISOMsg received = new ISOMsg();
            received.setPackager(packager);
            received.unpack(ISOUtil.hex2byte(hex));
            System.out.println("read back: " + received.getMTI() + " pan " + received.getString(2) + " terminal [" + received.getString(41) + "]");
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
