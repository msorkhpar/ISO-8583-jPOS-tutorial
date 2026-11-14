import com.example.practice.ClearingMatcher;
import com.example.practice.ClearingMatcher.Item;
import com.example.practice.ClearingMatcher.Result;
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
            List<Item> journal = List.of(new Item("31000007", "700101000101", 2450),
                    new Item("31000007", "700101000102", 8000), new Item("31000008", "700101000101", 1500),
                    new Item("31000008", "700101000105", 990));
            List<Item> clearing = List.of(new Item("31000008", "700101000101", 1500),
                    new Item("31000007", "700101000102", 9200), new Item("31000007", "700101000101", 2450),
                    new Item("31000007", "700101000101", 2450), new Item("31000009", "700101000777", 300));
            for (Result r : ClearingMatcher.match(journal, clearing))
                System.out.println(r.kind() + ": ours=" + r.ours() + " theirs=" + r.theirs());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
