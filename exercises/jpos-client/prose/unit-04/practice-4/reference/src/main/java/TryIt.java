import com.example.practice.PaymentService;
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
            host(sp, new AtomicReference<>("00"));
            sp.out("host.ready", Boolean.TRUE);
            PaymentService service = new PaymentService("issuer-mux", 1000);
            System.out.println("nothing deployed yet: " + service.pay(purchase("008801")));
            QMUX first = mux("tspace:tryit", "issuer-mux");
            try {
                System.out.println("after deploy: " + service.pay(purchase("008802")));
            } finally {
                first.stop();
                first.destroy();
            }
            System.out.println("undeployed: " + service.pay(purchase("008803")));
            QMUX second = mux("tspace:tryit", "issuer-mux");
            try {
                System.out.println("after redeploy: " + service.pay(purchase("008804")));
            } finally {
                second.stop();
                second.destroy();
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String stan) {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4000001240000000");
        m.set(3, "000000");
        m.set(4, "000000003310");
        m.set(11, stan);
        m.set(41, "30220044");
        return m;
    }

    private static QMUX mux(String space, String name) throws Exception {
        Element e = new Element("mux");
        e.addContent(new Element("space").setText(space));
        e.addContent(new Element("in").setText("host-receive"));
        e.addContent(new Element("out").setText("host-send"));
        e.addContent(new Element("ready").setText("host.ready"));
        QMUX q = new QMUX();
        q.setName(name);
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
