import com.example.practice.CheckoutClient;
import org.jdom2.Element;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.q2.iso.QMUX;
import org.jpos.space.LocalSpace;
import org.jpos.space.SpaceFactory;
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
            @SuppressWarnings("unchecked")
            LocalSpace<String, Object> sp = (LocalSpace<String, Object>) SpaceFactory.getSpace("tspace:tryit");
            Element e = new Element("mux");
            e.addContent(new Element("space").setText("tspace:tryit"));
            e.addContent(new Element("in").setText("host-receive"));
            e.addContent(new Element("out").setText("host-send"));
            e.addContent(new Element("ready").setText("host-channel.ready"));
            QMUX mux = new QMUX();
            mux.setName("tryit-mux");
            mux.setPersist(e);
            mux.setConfiguration(new SimpleConfiguration());
            mux.init();
            mux.start();
            approveAll(sp);
            try {
                CheckoutClient client = new CheckoutClient(mux, sp, 5000);
                System.out.println("link down: " + client.authorize(purchase("000601")));
                Date connection = new Date(1_763_000_000_000L);
                sp.out("host-channel.ready", connection);
                System.out.println("connected, no sign-on: " + client.authorize(purchase("000602")));
                sp.put("tryit-mux.enabled", connection);
                System.out.println("signed on: " + client.authorize(purchase("000603")));
                sp.inp("host-channel.ready");
                sp.out("host-channel.ready", new Date(1_763_000_090_000L));
                System.out.println("reconnected: " + client.authorize(purchase("000604")));
            } finally {
                mux.stop();
                mux.destroy();
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, "000000004250");
        m.set(11, stan);
        m.set(41, "31415926");
        return m;
    }

    @SuppressWarnings("unchecked")
    private static void approveAll(LocalSpace<String, Object> sp) {
        sp.addListener("host-send", (queue, value) -> {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            try {
                ISOMsg response = (ISOMsg) request.clone();
                response.setResponseMTI();
                response.set(39, "00");
                sp.out("host-receive", response);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }
}
