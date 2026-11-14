import com.example.practice.LinkPolicy;
import java.util.ArrayList;
import java.util.List;
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
            LinkPolicy policy = new LinkPolicy(5_000, 40_000);
            long now = 1_000_000;
            System.out.println("may sign on at start: " + policy.maySignOn(now));
            List<Long> waits = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                long next = policy.refused(now);
                waits.add(next - now);
                now = next;
            }
            System.out.println("waits after 6 refusals: " + waits);
            System.out.println("may sign on 1 ms early: " + policy.maySignOn(now - 1));
            policy.signedOn();
            System.out.println("may sign on after success: " + policy.maySignOn(now - 1));
            System.out.println("next outage, first wait: " + (policy.refused(now) - now));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
