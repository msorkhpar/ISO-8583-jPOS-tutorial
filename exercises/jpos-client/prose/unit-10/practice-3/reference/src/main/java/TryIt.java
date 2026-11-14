import com.example.practice.LoadReport;
import com.example.practice.LoadReport.Sample;
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
            List<Sample> samples = new ArrayList<>();
            for (int i = 1; i <= 50; i++)
                samples.add(new Sample(i == 25 ? 2_000_000L : 800L + 20L * i, i != 25));
            LoadReport r = LoadReport.of(samples);
            System.out.println("count: " + r.count);
            System.out.println("nulls: " + r.nulls);
            System.out.println("p50: " + r.p50);
            System.out.println("p95: " + r.p95);
            System.out.println("max: " + r.max);
            System.out.println("report: " + r);
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
