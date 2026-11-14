import com.example.practice.TicketSearch;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOMsg;

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
            List<ISOMsg> log = new ArrayList<>();
            exchange(log, "TERM1501", "000077", "0612143005", "00");
            exchange(log, "TERM1502", "000077", "0612143005", "00");
            exchange(log, "TERM1501", "000078", "0612143140", "51");
            exchange(log, "TERM1501", "000077", "0613080012", "00");
            TicketSearch.Ticket ticket = new TicketSearch.Ticket("TERM1501", "77", Instant.parse("2026-06-12T14:30:05Z"));
            List<ISOMsg> found = TicketSearch.find(log, ticket);
            System.out.println("found for TERM1501, STAN 77, 14:30:05 UTC: " + found.size());
            for (ISOMsg m : found)
                System.out.println("  " + m.getMTI() + " 7=" + m.getString(7) + " 11=" + m.getString(11) + " 39=" + m.getString(39));
            System.out.println("found for a message never logged: "
                    + TicketSearch.find(log, new TicketSearch.Ticket("TERM1501", "79", Instant.parse("2026-06-12T14:33:00Z"))).size());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static void exchange(List<ISOMsg> log, String terminal, String stan, String de7, String rc) throws Exception {
        ISOMsg request = new ISOMsg("0200");
        request.set(4, "000000002110");
        request.set(7, de7);
        request.set(11, stan);
        request.set(41, terminal);
        ISOMsg answer = (ISOMsg) request.clone();
        answer.setResponseMTI();
        answer.set(39, rc);
        log.add(request);
        log.add(answer);
    }
}
