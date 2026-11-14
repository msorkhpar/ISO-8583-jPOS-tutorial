import com.example.practice.LinkPolicy;
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
            LinkPolicy policy = new LinkPolicy(5_000, 40_000, 2);
            policy.signedOn();
            System.out.println("Monday, echo lost: " + policy.echoFailed());
            policy.echoAnswered();
            System.out.println("Wednesday, echo lost: " + policy.echoFailed());
            policy.echoAnswered();
            System.out.println("Friday, echo lost: " + policy.echoFailed());
            System.out.println("Friday, next echo lost too: " + policy.echoFailed());
            policy.signedOn();
            System.out.println("after a new sign-on, echo lost: " + policy.echoFailed());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
