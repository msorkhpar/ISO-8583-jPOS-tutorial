import com.example.practice.DatasetFinder;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

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
            ISOMsg m = new ISOMsg("2100");
            m.set(11, "000000170101");
            m.set(104, ISOUtil.hex2byte("010008" + "DF200177" + "9F350122" + "740002" + "C0DE"));
            m.set(55, ISOUtil.hex2byte("9F2701" + "40" + "9F360200A5"));
            for (String path : new String[] { "104.0x01.0xDF20", "104.0x01.0x9F35", "104.0x74", "104.0x02", "55.0x9F27", "55.0x9F36" }) {
                System.out.println(path + " = " + DatasetFinder.find(m, path));
            }
            try {
                DatasetFinder.find(m, "104.0x74.0x01");
            } catch (IllegalArgumentException refused) {
                System.out.println("104.0x74.0x01 refused: " + refused.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
