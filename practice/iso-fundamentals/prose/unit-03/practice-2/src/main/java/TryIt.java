import com.example.practice.FieldWriter;
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
            System.out.println("n6 [" + FieldWriter.write("n6", "1234") + "]");
            System.out.println("ans10 [" + FieldWriter.write("ans10", "SHOP 7") + "]");
            System.out.println("n..19 [" + FieldWriter.write("n..19", "4000123412341234") + "]");
            System.out.println("ans...999 [" + FieldWriter.write("ans...999", "NOTE=HELLO") + "]");
            System.out.println("a3 [" + FieldWriter.write("a3", "USD") + "]");
            try {
                System.out.println("n4 [" + FieldWriter.write("n4", "12345") + "]");
            } catch (IllegalArgumentException refused) {
                System.out.println("n4 refused: " + refused.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
