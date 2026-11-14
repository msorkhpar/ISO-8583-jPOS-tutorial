import com.example.practice.Upstream;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
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
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        try {
            Host host = new Host();
            Upstream gw = new Upstream(host, "GW000009");
            ISOMsg a = gw.forward(purchase("40210001", "000088"));
            ISOMsg b = gw.forward(purchase("40210002", "000088"));
            System.out.println("host saw: 41=" + host.received.get(0).getString(41) + " 11=" + host.received.get(0).getString(11)
                    + ", 41=" + host.received.get(1).getString(41) + " 11=" + host.received.get(1).getString(11));
            System.out.println("terminal 1 got: " + a.getMTI() + " 39=" + a.getString(39) + " 41=" + a.getString(41) + " 11=" + a.getString(11));
            System.out.println("terminal 2 got: " + b.getMTI() + " 39=" + b.getString(39) + " 41=" + b.getString(41) + " 11=" + b.getString(11));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String terminal, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, "000000003150");
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    /** A host that approves at once and keeps what it received. */
    static final class Host implements MUX {
        final List<ISOMsg> received = new ArrayList<>();

        @Override public ISOMsg request(ISOMsg m, long timeout) {
            received.add(m);
            ISOMsg r = (ISOMsg) m.clone();
            try { r.setResponseMTI(); r.set(39, "00"); } catch (Exception e) { throw new IllegalStateException(e); }
            return r;
        }
        @Override public void request(ISOMsg m, long t, ISOResponseListener l, Object h) { l.responseReceived(request(m, t), h); }
        @Override public void send(ISOMsg m) { received.add(m); }
        @Override public boolean isConnected() { return true; }
    }
}
