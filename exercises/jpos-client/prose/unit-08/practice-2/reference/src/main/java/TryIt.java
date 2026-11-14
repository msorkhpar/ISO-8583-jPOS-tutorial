import com.example.practice.DeclineReport;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.ProtectedLogListener;
import org.jpos.util.SimpleLogListener;
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
            ProtectedLogListener protect = new ProtectedLogListener();
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("protect", "2 35 45");
            cfg.put("wipe", "52 55");
            protect.setConfiguration(cfg);
            org.jpos.util.Logger node = new org.jpos.util.Logger();
            node.addListener(protect);
            node.addListener(new SimpleLogListener(System.out));
            ISOMsg request = new ISOMsg("0200");
            request.set(2, "4000002760003184");
            request.set(3, "000000");
            request.set(4, "000000004410");
            request.set(11, "088120");
            request.set(35, "4000002760003184=31052011000000000000");
            request.set(41, "50030007");
            request.set(52, ISOUtil.hex2byte("1A2B3C4D5E6F7081"));
            ISOMsg rejection = (ISOMsg) request.clone();
            rejection.setResponseMTI();
            rejection.set(39, "30");
            String dump = new DeclineReport(node, new GenericPackager("jar:packager/iso87ascii.xml")).report(request, rejection);
            System.out.println("dump for the support desk:");
            System.out.println(dump);
            System.out.println("request still has PAN: " + request.getString(2));
            System.out.println("request still has PIN block: " + ISOUtil.hexString(request.getBytes(52)));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
