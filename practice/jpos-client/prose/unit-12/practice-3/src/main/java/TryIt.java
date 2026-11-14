import com.example.practice.AdviceSaf;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
            Host host = new Host();
            host.script.addAll(List.of("drop", "drop", "00", "05"));
            AdviceSaf saf = new AdviceSaf(new AdviceSaf.Store());
            saf.add(advice("000411"));
            saf.add(advice("000412"));
            int attempts = 0;
            while (!saf.isEmpty() && attempts < 10) {
                System.out.println("attempt " + (++attempts) + " answered: " + saf.sendOldest(host, 1000));
            }
            System.out.println("host saw: " + host.seen);
            System.out.println("empty: " + saf.isEmpty());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg advice(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0220");
        m.set(2, "5500000000000004");
        m.set(4, "000000006125");
        m.set(11, stan);
        m.set(41, "31415926");
        return m;
    }

    /** A host that answers with the scripted response codes ("drop": no answer) and logs what it saw. */
    static final class Host implements MUX {
        final Deque<String> script = new ArrayDeque<>();
        final List<String> seen = new ArrayList<>();

        @Override public ISOMsg request(ISOMsg m, long timeout) {
            String code = script.isEmpty() ? "00" : script.poll();
            try {
                seen.add(m.getMTI() + ":" + m.getString(11));
                if (code.equals("drop")) return null;
                ISOMsg r = (ISOMsg) m.clone();
                r.setResponseMTI();
                r.set(39, code);
                return r;
            } catch (Exception e) { throw new IllegalStateException(e); }
        }
        @Override public void request(ISOMsg m, long t, ISOResponseListener l, Object h) { l.responseReceived(request(m, t), h); }
        @Override public void send(ISOMsg m) { }
        @Override public boolean isConnected() { return true; }
    }
}
