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
            PayoutProcessor processor = new PayoutProcessor(ledger);
            String m1 = "000000004433001", m2 = "000000004433002";
            run(processor, "run-1114.pay", "H|ACQ318|1114\nP|" + m1 + "|000000012500\nP|" + m2 + "|000000000475\nT|2|12975\n");
            run(processor, "run-1115.pay", "H|ACQ318|1115\nP|" + m1 + "|000000003000\n");
            run(processor, "run-1116.pay", "");
            run(processor, "run-1117.pay", "H|ACQ318|1117\nP|" + m1 + "|30,00\nT|1|3000\n");
            run(processor, "run-1118.pay", "H|ACQ318|1118\nP|" + m1 + "|000000003000\nP|" + m2 + "|000000001000\nT|2|5000\n");
            System.out.println("ledger payments: " + ledger.payments().size());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static void run(PayoutProcessor processor, String name, String content) {
        try {
            byte[] answer = processor.process(name, content.getBytes(StandardCharsets.US_ASCII));
            System.out.print(name + ": " + new String(answer, StandardCharsets.US_ASCII));
        } catch (DirPoll.DirPollException e) {
            System.out.println(name + ": " + (e.isRetry() ? "retry later, " : "to bad/, ") + e.getMessage());
        }
    }
}
