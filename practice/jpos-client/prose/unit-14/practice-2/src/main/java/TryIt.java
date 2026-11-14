import com.example.practice.Checkout;
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
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        try {
            TSpace<String, Object> sp = new TSpace<>();
            Pool pool = new Pool();
            pool.script.addAll(List.of("00", "51", "drop"));
            Checkout checkout = new Checkout(pool, sp, 500);
            System.out.println("answered 00: " + checkout.pay(purchase("002201", "000000000995")));
            System.out.println("answered 51: " + checkout.pay(purchase("002202", "000000250000")));
            System.out.println("no answer: " + checkout.pay(purchase("002203", "000000003340")));
            ISOMsg queued = (ISOMsg) sp.inp(Checkout.TO_REVERSE);
            System.out.println("to reverse: " + (queued == null ? "nothing" : queued.getString(11)));
            pool.connected = false;
            System.out.println("all links down: " + checkout.pay(purchase("002204", "000000000500")));
            System.out.println("sent to pool: " + pool.received + ", still to reverse: " + (sp.rdp(Checkout.TO_REVERSE) != null));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String stan, String amount) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(7, "1114121530");
        m.set(11, stan);
        m.set(41, "27180001");
        return m;
    }

    /** Stands in for the pool: answers with scripted codes ("drop": no answer), counts what it got. */
    static final class Pool implements MUX {
        final Deque<String> script = new ArrayDeque<>();
        boolean connected = true;
        int received;

        @Override public ISOMsg request(ISOMsg m, long timeout) {
            received++;
            String code = script.isEmpty() ? "00" : script.poll();
            if (code.equals("drop")) return null;
            ISOMsg r = (ISOMsg) m.clone();
            try { r.setResponseMTI(); r.set(39, code); } catch (Exception e) { throw new IllegalStateException(e); }
            return r;
        }
        @Override public void request(ISOMsg m, long t, ISOResponseListener l, Object h) { l.responseReceived(request(m, t), h); }
        @Override public void send(ISOMsg m) { received++; }
        @Override public boolean isConnected() { return connected; }
    }
}
