import com.example.practice.BatchNumbers;
import org.jpos.space.TSpace;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
            TSpace<String, Object> sp = new TSpace<>();
            sp.put("profile.60400133", new BatchNumbers.Profile("60400133", "000000009310255", 118));
            BatchNumbers batches = new BatchNumbers(sp);
            System.out.println("first settlement: batch " + batches.nextBatch("60400133"));
            System.out.println("second settlement: batch " + batches.nextBatch("60400133"));
            Set<Integer> got = ConcurrentHashMap.newKeySet();
            Thread a = new Thread(() -> got.add(batches.nextBatch("60400133")));
            Thread b = new Thread(() -> got.add(batches.nextBatch("60400133")));
            a.start(); b.start();
            a.join(5000); b.join(5000);
            System.out.println("two sessions at once got: " + got);
            BatchNumbers.Profile profile = batches.profile("60400133");
            System.out.println("stored profile: batch " + profile.batch + ", merchant " + profile.merchant);
            System.out.println("profiles in the space: " + sp.size("profile.60400133"));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
