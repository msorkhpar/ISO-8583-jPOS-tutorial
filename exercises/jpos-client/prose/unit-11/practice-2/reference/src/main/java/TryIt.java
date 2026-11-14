import com.example.practice.LinkSession;
import org.jpos.space.TSpace;
import java.util.ArrayList;
import java.util.Date;
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
            List<String> codes = new ArrayList<>();
            LinkSession s = new LinkSession(sp, "east-mux", "east-channel.ready",
                    (mux, code) -> codes.add(code), 3_600_000L, 86_400_000L);
            Date connection = new Date(1_763_100_000_000L);
            sp.out("east-channel.ready", connection);
            System.out.println("step: " + s.step());
            System.out.println("enabled before stop: " + (sp.rdp("east-mux.enabled") != null));
            s.signOff();
            System.out.println("codes sent: " + codes);
            System.out.println("enabled after stop: " + (sp.rdp("east-mux.enabled") != null));
            System.out.println("deployed again: " + s.step());
            System.out.println("enabled again: " + (sp.rdp("east-mux.enabled") == connection));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
