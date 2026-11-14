import com.example.practice.DumpReader;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;

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
            ISOMsg m = new ISOMsg("0200");
            m.set(2, "4111111111111111");
            m.set(3, "000000");
            m.set(4, "000000002550");
            m.set(11, "734120");
            m.set(41, "TERM0042");
            m.set(49, "840");
            DumpReader single = new DumpReader(capture(m));
            System.out.println("mti: " + single.mti());
            System.out.println("fields: " + single.fields());
            System.out.println("dataStart: " + single.dataStart());

            m.set(70, "301");
            DumpReader twoMaps = new DumpReader(capture(m));
            System.out.println("fields with a second bitmap: " + twoMaps.fields());
            System.out.println("dataStart with a second bitmap: " + twoMaps.dataStart());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static String capture(ISOMsg m) throws Exception {
        m.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        return new String(m.pack());
    }
}
