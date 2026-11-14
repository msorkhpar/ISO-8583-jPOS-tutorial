import com.example.practice.StuckWatch;

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
            StuckWatch night = new StuckWatch(20_000);
            System.out.println("night, idle at 0 s: " + night.poll(97, 97, 0));
            System.out.println("night, idle at 60 s: " + night.poll(97, 97, 60_000));
            System.out.println("night, 97 running at 75 s: " + night.poll(98, 97, 75_000));
            System.out.println("night, 97 still running at 100 s: " + night.poll(98, 97, 100_000));
            StuckWatch lunch = new StuckWatch(20_000);
            long[][] polls = { { 1520, 1511 }, { 1534, 1522 }, { 1549, 1530 }, { 1561, 1544 } };
            for (int i = 0; i < polls.length; i++)
                System.out.println("lunch, poll at " + i * 15 + " s: " + lunch.poll(polls[i][0], polls[i][1], i * 15_000L));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
