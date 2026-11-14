import com.example.practice.AskIssuer;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.jpos.rc.Result;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
import org.jpos.util.NameRegistrar;

import java.util.Date;
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
            NameRegistrar.register("mux.tryit-issuer", new MUX() {
                @Override
                public ISOMsg request(ISOMsg m, long timeout) {
                    System.out.println("  issuer asked about STAN " + m.getString(11) + " with " + timeout / 1000 + " s to answer");
                    ISOMsg r = (ISOMsg) m.clone();
                    r.set(0, "0210");
                    r.set(39, "00");
                    return r;
                }
                @Override public void request(ISOMsg m, long t, ISOResponseListener l, Object h) { }
                @Override public void send(ISOMsg m) { }
                @Override public boolean isConnected() { return true; }
            });
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("mux", "tryit-issuer");
            cfg.put("terminal-wait", "20000");
            AskIssuer ask = new AskIssuer();
            ask.setConfiguration(cfg);
            System.out.println("arrived just now: " + run(ask, "007301", 0));
            System.out.println("arrived 12 s ago: " + run(ask, "007302", 12_000));
            System.out.println("arrived 25 s ago: " + run(ask, "007303", 25_000));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            NameRegistrar.unregister("mux.tryit-issuer");
        }
    }

    private static String run(AskIssuer ask, String stan, long ageMs) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4012888888881881");
        m.set(3, "010000");
        m.set(4, "000000006000");
        m.set(11, stan);
        m.set(41, "ATM00562");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        ctx.put(ContextConstants.TIMESTAMP.toString(), new Date(System.currentTimeMillis() - ageMs));
        int vote = ask.prepare(1L, ctx);
        if ((vote & TransactionConstants.PREPARED) != 0) {
            ISOMsg answer = ctx.get(AskIssuer.ISSUER_RESPONSE);
            return "goes on, issuer answered " + (answer == null ? null : answer.getString(39));
        }
        Result.Entry failure = ctx.getResult().failure();
        return "stops, cause " + (failure == null ? "none recorded" : failure.getIrc());
    }
}
