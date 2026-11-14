import com.example.practice.AtcGuard;
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
            AtcGuard guard = new AtcGuard();
            String pan = "4012888888881881";
            System.out.println("card 000, ATC 0x0019: " + guard.check(request(pan, "000", "0019"), true));
            System.out.println("card 000, ATC 0x001A: " + guard.check(request(pan, "000", "001A"), true));
            System.out.println("card 000, ATC 0x001A again: " + guard.check(request(pan, "000", "001A"), true));
            System.out.println("card 000, ATC 0x7000, bad cryptogram: " + guard.check(request(pan, "000", "7000"), false));
            System.out.println("card 000, ATC 0x001B: " + guard.check(request(pan, "000", "001B"), true));
            System.out.println("replacement card 001, ATC 0x0002: " + guard.check(request(pan, "001", "0002"), true));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg request(String pan, String psn, String atcHex) throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(2, pan);
        m.set(4, "000000001875");
        m.set(23, psn);
        m.set(55, ISOUtil.hex2byte("9F2608A1B2C3D4E5F60718" + "9F270180" + "9F3602" + atcHex));
        return m;
    }
}
