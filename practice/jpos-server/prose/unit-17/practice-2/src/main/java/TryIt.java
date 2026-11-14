import com.example.practice.PayoutProcessor;
import org.jpos.util.DirPoll;

import java.nio.charset.StandardCharsets;
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
            PayoutProcessor.Ledger ledger = new PayoutProcessor.Ledger();
            String run1114 = "H|ACQ318|1114\nP|000000004433001|000000012500\nP|000000004433002|000000000475\nT|2|12975\n";
            run(new PayoutProcessor(ledger), "run-1114.pay", run1114);
            run(new PayoutProcessor(ledger), "run-1114.pay", run1114);
            run(new PayoutProcessor(ledger), "resent-1114.pay", run1114);
            run(new PayoutProcessor(ledger), "acq555-1114.pay", "H|ACQ555|1114\nP|000000009900001|000000000800\nT|1|800\n");
            run(new PayoutProcessor(ledger), "run-1114b.pay", "H|ACQ318|1114\nP|000000004433001|000000099999\nT|1|99999\n");
            System.out.println("ledger payments: " + ledger.payments().size());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    /** One file reaching a processor; a new processor each time stands for a restarted node. */
    private static void run(PayoutProcessor processor, String name, String content) {
        try {
            byte[] answer = processor.process(name, content.getBytes(StandardCharsets.US_ASCII));
            System.out.print(name + ": " + new String(answer, StandardCharsets.US_ASCII));
        } catch (DirPoll.DirPollException e) {
            System.out.println(name + ": " + (e.isRetry() ? "retry later, " : "to bad/, ") + e.getMessage());
        }
    }
}
