import com.example.practice.PaymentEndpoint;
import java.util.concurrent.atomic.AtomicReference;
import org.jdom2.Element;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.q2.iso.QMUX;
import org.jpos.space.LocalSpace;
import org.jpos.space.SpaceFactory;
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
            LocalSpace<String, Object> sp = (LocalSpace<String, Object>) SpaceFactory.getSpace("tspace:tryit");
            AtomicReference<String> rc = new AtomicReference<>("00");
            QMUX q = mux("tspace:tryit");
            try {
                host(sp, rc);
                sp.out("host.ready", Boolean.TRUE);
                PaymentEndpoint api = new PaymentEndpoint(q, 400);
                System.out.println("approved: " + api.handle(purchase("006001", "000000001550")));
                rc.set("51");
                System.out.println("insufficient funds: " + api.handle(purchase("006002", "000000990000")));
                rc.set(null);
                System.out.println("host silent: " + api.handle(purchase("006003", "000000002000")));
                sp.inp("host.ready");
                System.out.println("link down: " + api.handle(purchase("006004", "000000000700")));
            } finally {
                q.stop();
                q.destroy();
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String stan, String amount) {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4000002760003184");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "30220010");
        return m;
    }

    private static QMUX mux(String space) throws Exception {
        Element e = new Element("mux");
        e.addContent(new Element("space").setText(space));
        e.addContent(new Element("in").setText("host-receive"));
        e.addContent(new Element("out").setText("host-send"));
        e.addContent(new Element("ready").setText("host.ready"));
        QMUX q = new QMUX();
        q.setName("tryit-mux");
        q.setPersist(e);
        q.setConfiguration(new SimpleConfiguration());
        q.init();
        q.start();
        return q;
    }

    private static void host(LocalSpace<String, Object> sp, AtomicReference<String> rc) {
        sp.addListener("host-send", (key, value) -> {
            ISOMsg request = (ISOMsg) sp.inp(key);
            if (request == null || rc.get() == null)
                return;
            try {
                ISOMsg response = (ISOMsg) request.clone();
                response.setResponseMTI();
                response.set(39, rc.get());
                sp.out("host-receive", response);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }
}
