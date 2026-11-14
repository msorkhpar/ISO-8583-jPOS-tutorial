import com.example.practice.LinkMessages;
import org.jpos.iso.ISOMsg;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
            Clock clock = Clock.fixed(Instant.parse("2026-11-14T08:05:30Z"), ZoneOffset.UTC);
            LinkMessages link = new LinkMessages("5520017", "POS00911", 999998, clock);
            ISOMsg echo = link.echo();
            System.out.println("echo: " + echo.getMTI() + " 70=" + echo.getString(70) + " 7=" + echo.getString(7) + " 11=" + echo.getString(11));
            ISOMsg notice = link.signOffNotice();
            notice.set(48, "MAINTENANCE 0300-0330");
            System.out.println("notice: " + notice.getMTI() + " 70=" + notice.getString(70) + " 11=" + notice.getString(11));
            ISOMsg text = link.merchantText("New receipts from Monday");
            System.out.println("text: " + text.getMTI() + " 11=" + text.getString(11) + " 63=" + text.getString(63));
            ISOMsg again = link.signOffNotice();
            System.out.println("second notice: 11=" + again.getString(11) + " 48=" + again.getString(48));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
