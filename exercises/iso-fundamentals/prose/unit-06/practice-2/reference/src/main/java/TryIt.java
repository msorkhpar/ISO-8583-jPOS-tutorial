import com.example.practice.Field48Reader;
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
            String withCategory = "U" + "4206774201" + "3304ABCD" + "4202OK";
            System.out.println("with category code: " + Field48Reader.wanted(withCategory, "42", "33", "77"));
            String withoutCategory = "3304WXYZ" + "4203555";
            System.out.println("without category code: " + Field48Reader.wanted(withoutCategory, "42", "33"));
            System.out.println("a tag it doesn't carry: " + Field48Reader.wanted(withoutCategory, "61"));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
