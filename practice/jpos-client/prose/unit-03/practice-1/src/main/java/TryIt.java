import com.example.practice.FrameReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
            ByteArrayOutputStream wire = new ByteArrayOutputStream();
            frame(wire, new byte[0]);
            frame(wire, "0800 STAN 120501".getBytes(StandardCharsets.US_ASCII));
            frame(wire, new byte[300]);
            frame(wire, "0810 STAN 120502".getBytes(StandardCharsets.US_ASCII));
            byte[] bytes = wire.toByteArray();
            FrameReader reader = new FrameReader(new ByteArrayInputStream(bytes));
            System.out.println("first: " + new String(reader.next(), StandardCharsets.US_ASCII));
            System.out.println("second, length: " + reader.next().length);
            System.out.println("third: " + new String(reader.next(), StandardCharsets.US_ASCII));
            FrameReader cut = new FrameReader(new ByteArrayInputStream(Arrays.copyOf(bytes, 25)));
            cut.next();
            try {
                cut.next();
                System.out.println("cut stream: no error");
            } catch (java.io.IOException e) {
                System.out.println("cut stream: " + e);
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static void frame(ByteArrayOutputStream out, byte[] message) {
        out.write(message.length >> 8);
        out.write(message.length & 0xFF);
        out.write(message, 0, message.length);
    }
}
