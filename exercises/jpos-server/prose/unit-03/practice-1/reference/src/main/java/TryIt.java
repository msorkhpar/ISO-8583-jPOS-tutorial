import com.example.practice.MaintenanceListener;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
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
            MaintenanceListener listener = new MaintenanceListener();
            System.out.println("0200 before maintenance: " + offer(listener, message("0200", "001201")));
            listener.begin();
            System.out.println("0200 in maintenance: " + offer(listener, message("0200", "001202")));
            System.out.println("0100 in maintenance: " + offer(listener, message("0100", "001203")));
            System.out.println("0800 in maintenance: " + offer(listener, message("0800", "001204")));
            System.out.println("0420 in maintenance: " + offer(listener, message("0420", "001205")));
            listener.end();
            System.out.println("0200 after maintenance: " + offer(listener, message("0200", "001206")));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg message(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        if (mti.startsWith("08")) {
            m.set(70, "301");
        } else {
            m.set(2, "4000056655665556");
            m.set(3, "000000");
            m.set(4, "000000004990");
        }
        m.set(7, "1114230500");
        m.set(11, stan);
        m.set(41, "POS00915");
        return m;
    }

    /** Offers one message to the listener, then to the queue, the way a QServer session does. */
    private static String offer(MaintenanceListener listener, ISOMsg m) throws Exception {
        String[] answer = { null };
        ISOSource terminal = new ISOSource() {
            @Override public void send(ISOMsg r) { answer[0] = r.getString(0) + " 39=" + r.getString(39) + " 11=" + r.getString(11); }
            @Override public boolean isConnected() { return true; }
        };
        if (listener.process(terminal, m))
            return "answered " + answer[0];
        return "passed on to IncomingListener";
    }
}
