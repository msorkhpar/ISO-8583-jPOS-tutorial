import com.example.practice.LayoutCheck;
import org.jpos.iso.IFA_NUMERIC;
import org.jpos.iso.packager.GenericPackager;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
            byte[] sample = ("0200" + "7020000000800000" + "16" + "4000056655665556"
                    + "000000" + "000000001450" + "000613" + "POS00071").getBytes(StandardCharsets.US_ASCII);
            Map<Integer, String> documented = Map.of(
                    2, "4000056655665556", 3, "000000", 4, "000000001450", 11, "000613", 41, "POS00071");
            GenericPackager good = new GenericPackager("jar:packager/iso87ascii.xml");
            System.out.println("host layout: " + LayoutCheck.firstWrongField(good, sample.clone(), documented));
            GenericPackager stanTooLong = new GenericPackager("jar:packager/iso87ascii.xml");
            stanTooLong.setFieldPackager(11, new IFA_NUMERIC(7, "SYSTEM TRACE AUDIT NUMBER"));
            System.out.println("field 11 one digit too long: " + LayoutCheck.firstWrongField(stanTooLong, sample.clone(), documented));
            GenericPackager noAmount = new GenericPackager("jar:packager/iso87ascii.xml");
            noAmount.setFieldPackager(4, null);
            System.out.println("no row for field 4: " + LayoutCheck.firstWrongField(noAmount, sample.clone(), documented));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
