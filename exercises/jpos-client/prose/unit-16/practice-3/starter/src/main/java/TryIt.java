import com.example.practice.LabProxy;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.ISOSource;
import org.jpos.iso.MUX;
import java.util.ArrayDeque;
import java.util.Deque;
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
            CertHost cert = new CertHost();
            cert.script.addAll(List.of("00", "51", "drop"));
            LabProxy proxy = new LabProxy(cert, r -> answer(r, "LAB001", "00"), 500);
            System.out.println("in slot, approved: " + tryOne(proxy, purchase("003401")));
            System.out.println("in slot, declined: " + tryOne(proxy, purchase("003402")));
            System.out.println("in slot, no answer: " + tryOne(proxy, purchase("003403")));
            cert.connected = false;
            System.out.println("after the slot: " + tryOne(proxy, purchase("003404")));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4000056655665556");
        m.set(3, "000000");
        m.set(4, "000000001840");
        m.set(11, stan);
        m.set(41, "27180012");
        return m;
    }

    private static ISOMsg answer(ISOMsg request, String approval, String rc) {
        ISOMsg r = (ISOMsg) request.clone();
        try { r.setResponseMTI(); } catch (Exception e) { throw new IllegalStateException(e); }
        r.set(38, approval);
        r.set(39, rc);
        return r;
    }

    /** The certification host: answers with scripted codes ("drop": nothing), or is down. */
    static final class CertHost implements MUX {
        final Deque<String> script = new ArrayDeque<>();
        boolean connected = true;

        @Override public ISOMsg request(ISOMsg m, long timeout) {
            String rc = script.isEmpty() ? "00" : script.poll();
            return rc.equals("drop") ? null : answer(m, "C77120", rc);
        }
        @Override public void request(ISOMsg m, long t, ISOResponseListener l, Object h) { l.responseReceived(request(m, t), h); }
        @Override public void send(ISOMsg m) { }
        @Override public boolean isConnected() { return connected; }
    }

    /** What the tester's client gets back for one purchase. */
    private static String tryOne(LabProxy proxy, ISOMsg request) {
        String[] got = { "nothing" };
        proxy.process(new ISOSource() {
            @Override public void send(ISOMsg r) { got[0] = "38=" + r.getString(38) + " 39=" + r.getString(39); }
            @Override public boolean isConnected() { return true; }
        }, request);
        return got[0];
    }
}
