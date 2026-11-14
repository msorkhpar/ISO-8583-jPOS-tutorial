import com.example.practice.RedeemPoints;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.util.NameRegistrar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
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
            AtomicInteger ids = new AtomicInteger(500);
            NameRegistrar.register("tryit-points", new RedeemPoints.PointsService() {
                @Override public String redeem(String account, long points) {
                    String id = "RDM-" + ids.incrementAndGet();
                    System.out.println("  service: redeem " + points + " from " + account + " -> " + id);
                    return id;
                }
                @Override public void confirm(String id) { System.out.println("  service: confirm " + id); }
                @Override public void cancel(String id) { System.out.println("  service: cancel " + id); }
            });
            System.out.println("approved, 1200 points:");
            redeemPoints().commit(1L, prepared("LOY-8810", "1200"));
            System.out.println("declined later, 300 points:");
            redeemPoints().abort(2L, prepared("LOY-8811", "300"));
            System.out.println("crash after redeeming 800 points, then restart:");
            redeemPoints().abort(3L, recovered(prepared("LOY-8812", "800")));
            System.out.println("crash during a purchase without points, then restart:");
            redeemPoints().abort(4L, recovered(prepared("LOY-8813", null)));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            NameRegistrar.unregister("tryit-points");
        }
    }

    private static RedeemPoints redeemPoints() {
        RedeemPoints participant = new RedeemPoints();
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("service", "tryit-points");
        participant.setConfiguration(cfg);
        return participant;
    }

    private static Context prepared(String account, String points) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4024007103939509");
        m.set(3, "000000");
        m.set(4, "000000005400");
        m.set(11, "009102");
        m.set(41, "60400140");
        if (points != null) m.set(48, points);
        m.set(102, account);
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        redeemPoints().prepare(1L, ctx);
        return ctx;
    }

    /** What a restarted TransactionManager reads back from its journal. */
    private static Context recovered(Context ctx) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(ctx);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (Context) in.readObject();
        }
    }
}
