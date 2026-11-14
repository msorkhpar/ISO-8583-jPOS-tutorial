import com.example.practice.HoldRouter;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.jpos.space.TSpace;
import java.util.ArrayList;
import java.util.List;
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
            DataCentre east = new DataCentre();
            DataCentre west = new DataCentre();
            HoldRouter router = new HoldRouter(new TSpace<>(), 3_600_000L, east, west);
            ISOMsg stayA = preauth("000120", "1110150000");
            ISOMsg stayB = preauth("000121", "1110151500");
            router.request(stayA, 1000);
            router.request(stayB, 1000);
            router.request(preauth("000122", "1110152000"), 1000);
            ISOMsg done = router.request(completion(stayB, "000130", "1114103000"), 1000);
            System.out.println("completion of B answered: " + done.getMTI());
            west.connected = false;
            System.out.println("completion of B with west down: " + router.request(completion(stayB, "000131", "1114103100"), 1000));
            System.out.println("completion of A: " + router.request(completion(stayA, "000132", "1114103200"), 1000).getMTI());
            System.out.println("east got: " + east.stans);
            System.out.println("west got: " + west.stans);
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg preauth(String stan, String time) throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(3, "000000");
        m.set(4, "000000045000");
        m.set(7, time);
        m.set(11, stan);
        m.set(41, "61000008");
        return m;
    }

    private static ISOMsg completion(ISOMsg p, String stan, String time) throws Exception {
        ISOMsg m = new ISOMsg("0220");
        m.set(3, "000000");
        m.set(4, "000000041280");
        m.set(7, time);
        m.set(11, stan);
        m.set(41, p.getString(41));
        m.set(90, "0100" + p.getString(11) + p.getString(7) + "0000000000000000000000");
        return m;
    }

    /** A data centre that approves everything and keeps the STANs it received. */
    static final class DataCentre implements MUX {
        final List<String> stans = new ArrayList<>();
        boolean connected = true;

        @Override public ISOMsg request(ISOMsg m, long timeout) {
            stans.add(m.getString(11));
            ISOMsg r = (ISOMsg) m.clone();
            try { r.setResponseMTI(); r.set(39, "00"); } catch (Exception e) { throw new IllegalStateException(e); }
            return r;
        }
        @Override public void request(ISOMsg m, long t, ISOResponseListener l, Object h) { l.responseReceived(request(m, t), h); }
        @Override public void send(ISOMsg m) { stans.add(m.getString(11)); }
        @Override public boolean isConnected() { return connected; }
    }
}
