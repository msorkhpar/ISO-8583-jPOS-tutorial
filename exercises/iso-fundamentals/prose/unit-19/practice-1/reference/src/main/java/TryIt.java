import com.example.practice.BusinessDay;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
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
            BusinessDay london = new BusinessDay(ZoneId.of("Europe/London"), LocalTime.of(16, 0),
                    Set.of(LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 28)));
            System.out.println("Thu 12 Nov 10:15 settles: " + london.settles(Instant.parse("2026-11-12T10:15:00Z")));
            System.out.println("Thu 12 Nov 16:40 settles: " + london.settles(Instant.parse("2026-11-12T16:40:00Z")));
            System.out.println("Fri 13 Nov 18:00 settles: " + london.settles(Instant.parse("2026-11-13T18:00:00Z")));
            System.out.println("Thu 9 Jul 15:20 UTC settles: " + london.settles(Instant.parse("2026-07-09T15:20:00Z")));
            System.out.println("DE 15, no answer, 3 Feb 09:00: " + london.de15For(null, Instant.parse("2027-02-03T09:00:00Z")));
            ISOMsg answer = new ISOMsg("0210");
            answer.set(15, "0205");
            answer.set(39, "00");
            System.out.println("DE 15, host said 0205: " + london.de15For(answer, Instant.parse("2027-02-03T09:00:00Z")));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
