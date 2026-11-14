import com.example.practice.HexDumpReader;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
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
            GenericPackager packager = new GenericPackager("jar:packager/iso87ascii.xml");
            ISOMsg auth = new ISOMsg("0100");
            auth.setPackager(packager);
            auth.set(3, "000000");
            auth.set(4, "000000007788");
            auth.set(11, "005150");
            auth.set(41, "27180020");
            byte[] bytes = auth.pack();
            String dump = ISOUtil.hexdump(bytes);
            System.out.print(dump);
            byte[] back = HexDumpReader.read(dump);
            System.out.println("read " + back.length + " of " + bytes.length + " bytes, same: " + Arrays.equals(bytes, back));
            ISOMsg m = new ISOMsg();
            m.setPackager(packager);
            m.unpack(back);
            System.out.println("MTI " + m.getMTI() + ", 11=" + m.getString(11) + ", 41=" + m.getString(41));
            String lost = dump.lines().filter(l -> !l.startsWith("0010")).reduce("", (a, b) -> a + b + "\n");
            try {
                System.out.println("line 0010 lost: read " + HexDumpReader.read(lost).length + " bytes");
            } catch (IllegalArgumentException e) {
                System.out.println("line 0010 lost: refused, " + e.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
