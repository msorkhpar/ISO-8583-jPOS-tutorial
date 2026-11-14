import com.example.practice.PaymentClient;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.jpos.space.TSpace;
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
            TSpace<String, Object> sp = new TSpace<>();
            Host host = new Host();
            host.script.addAll(List.of("00", "05", "drop", "drop", "drop"));
            PaymentClient client = new PaymentClient(host, sp, 1000);
            System.out.println("0200 answered 00: " + client.send(message("0200", "000901")));
            System.out.println("0200 answered 05: " + client.send(message("0200", "000902")));
            System.out.println("0200 no answer: " + client.send(message("0200", "000903")));
            ISOMsg rev = (ISOMsg) sp.inp("saf.host");
            System.out.println("queued: " + rev.getMTI() + " 90=" + rev.getString(90));
            System.out.println("0220 no answer: " + client.send(message("0220", "000904")));
            System.out.println("queued: " + ((ISOMsg) sp.inp("saf.host")).getMTI());
            System.out.println("0800 no answer: " + client.send(message("0800", "000905")));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg message(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "5500000000000004");
        m.set(3, "000000");
        m.set(4, "000000008800");
        m.set(7, "1114093000");
        m.set(11, stan);
        m.set(32, "654321");
        m.set(41, "31415926");
        return m;
    }

    /** A host that answers with the scripted response codes; "drop" means no answer. */
    static final class Host implements MUX {
        final Deque<String> script = new ArrayDeque<>();

        @Override public ISOMsg request(ISOMsg m, long timeout) {
            String code = script.isEmpty() ? "00" : script.poll();
            if (code.equals("drop")) return null;
            ISOMsg r = (ISOMsg) m.clone();
            try { r.setResponseMTI(); r.set(39, code); } catch (Exception e) { throw new IllegalStateException(e); }
            return r;
        }
        @Override public void request(ISOMsg m, long t, ISOResponseListener l, Object h) { l.responseReceived(request(m, t), h); }
        @Override public void send(ISOMsg m) { }
        @Override public boolean isConnected() { return true; }
    }
}
