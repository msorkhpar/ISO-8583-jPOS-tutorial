import com.example.practice.EntryModeScreen;
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
            EntryModeScreen screen = new EntryModeScreen();
            System.out.println("contactless with chip data: " + verdict(screen.screen(purchase("071", true, true))));
            System.out.println("stripe, no PIN pad: " + verdict(screen.screen(purchase("902", true, false))));
            System.out.println("chip code, track only: " + verdict(screen.screen(purchase("051", true, false))));
            System.out.println("stripe code with chip data: " + verdict(screen.screen(purchase("901", true, true))));
            System.out.println("entry mode 81: " + verdict(screen.screen(purchase("811", true, false))));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static String verdict(ISOMsg answer) throws Exception {
        return answer == null ? "goes upstream" : answer.getMTI() + " response code " + answer.getString(39);
    }

    private static ISOMsg purchase(String entryMode, boolean track2, boolean icc) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4761739001010010");
        m.set(3, "000000");
        m.set(4, "000000001275");
        m.set(11, "009904");
        m.set(22, entryMode);
        m.set(41, "40220077");
        if (track2) m.set(35, "4761739001010010=28122011143804400000");
        if (icc) m.set(55, new byte[] { (byte) 0x9F, 0x26, 0x02, 0x0A, 0x0B });
        return m;
    }
}
