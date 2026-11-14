import com.example.practice.CompositeWriter;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOUtil;

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
            byte[] small = CompositeWriter.write(List.of(
                    new CompositeWriter.Dataset(0x05, List.of(
                            new CompositeWriter.Element(0x9F1A, ISOUtil.hex2byte("0840")),
                            new CompositeWriter.Element(0xDF8101, ISOUtil.hex2byte("01")))),
                    new CompositeWriter.Dataset(0x0F, List.of())));
            System.out.println("two datasets: " + ISOUtil.hexString(small));
            byte[] payload = new byte[200];
            byte[] big = CompositeWriter.write(List.of(new CompositeWriter.Dataset(0x11,
                    List.of(new CompositeWriter.Element(0xDF30, payload)))));
            System.out.println("200-byte value, header: " + ISOUtil.hexString(big).substring(0, 14) + "..., " + big.length + " bytes");
            try {
                CompositeWriter.write(List.of(new CompositeWriter.Dataset(0x80, List.of())));
            } catch (IllegalArgumentException refused) {
                System.out.println("dataset 0x80 refused: " + refused.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
