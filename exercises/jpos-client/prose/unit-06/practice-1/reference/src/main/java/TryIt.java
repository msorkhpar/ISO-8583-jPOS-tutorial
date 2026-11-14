import com.example.practice.TerminalBridge;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
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
            String[] blocks = { "7A1B2C3D4E5F6071", "0F1E2D3C4B5A6978" };
            int[] next = { 0 };
            TerminalBridge bridge = new TerminalBridge(buffer ->
                    System.arraycopy(ISOUtil.hex2byte(blocks[next[0]++]), 0, buffer, 0, 8));
            ISOMsg first = bridge.purchase("4111111111111111", 5400, "021101", "9F2701409F360200A19F1A020250");
            ISOMsg second = bridge.purchase("5555555555554444", 3300, "021102", null);
            System.out.println("first PIN block: " + ISOUtil.hexString(first.getBytes(52)));
            System.out.println("second PIN block: " + ISOUtil.hexString(second.getBytes(52)));
            System.out.println("first chip data: " + ISOUtil.hexString(first.getBytes(55)));
            GenericPackager ascii = new GenericPackager("jar:packager/iso87ascii.xml");
            first.setPackager(ascii);
            ISOMsg atHost = new ISOMsg();
            atHost.setPackager(ascii);
            atHost.unpack(first.pack());
            System.out.println("chip data at the ASCII host: " + ISOUtil.hexString(atHost.getBytes(55)));
            System.out.println("second has chip data: " + second.hasField(55));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
