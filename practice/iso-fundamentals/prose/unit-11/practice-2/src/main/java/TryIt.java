import com.example.practice.RangeRouter;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main and see what prints. Submit grades. */
public class TryIt {
    /** Held here, so the JDK's own exit notice stays off: a logger nothing holds can be dropped. */
    private static final Logger JDK_EXIT = Logger.getLogger("java.lang.Runtime");

    public static void main(String[] args) {
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
            RangeRouter router = new RangeRouter();
            router.reload(List.of(
                    new RangeRouter.Range("34000000", "37999999", "network-a"),
                    new RangeRouter.Range("37144900", "37144963", "own-bank"),
                    new RangeRouter.Range("60110000", "60119999", "network-d")));
            for (String pan : List.of("371449635398431", "378282246310005", "6011000990139424", "3714490012345678", "5105105105105100")) {
                System.out.println(pan.substring(0, 8) + "... goes to " + router.destinationFor(pan));
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
