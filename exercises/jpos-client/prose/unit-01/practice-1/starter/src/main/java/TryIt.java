import com.example.practice.LinkDescriptors;
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
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        try {
            Element adaptor = LinkDescriptors.channelAdaptor("amex-primary", "to-amex", "from-amex", "127.0.0.1", 9300);
            Element mux = LinkDescriptors.muxFor(adaptor, "amex-mux");
            System.out.println("mux class: " + mux.getAttributeValue("class"));
            System.out.println("in/out: " + mux.getChildText("in") + " / " + mux.getChildText("out"));
            System.out.println("ready: " + mux.getChildText("ready") + ", unhandled: " + mux.getChildText("unhandled"));
            LocalSpace<String, Object> sp = (LocalSpace<String, Object>) SpaceFactory.getSpace("tspace:tryit");
            Element e = mux.clone();
            e.addContent(new Element("space").setText("tspace:tryit"));
            QMUX q = new QMUX();
            q.setName("amex-mux");
            q.setPersist(e);
            q.setConfiguration(new SimpleConfiguration());
            try {
                q.init();
                q.start();
                System.out.println("connected before the link is up: " + q.isConnected());
                sp.addListener("to-amex", (key, value) -> answer(sp, "from-amex", (ISOMsg) sp.inp(key)));
                sp.out("amex-primary.ready", new java.util.Date());
                ISOMsg echo = new ISOMsg("0800");
                echo.set(11, "000321");
                echo.set(70, "301");
                ISOMsg r = q.request(echo, 3000);
                System.out.println("echo answer: " + (r == null ? "none" : r.getMTI() + " STAN " + r.getString(11)));
            } finally {
                q.stop();
                q.destroy();
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static void answer(LocalSpace<String, Object> sp, String out, ISOMsg request) {
        if (request == null)
            return;
        try {
            ISOMsg response = (ISOMsg) request.clone();
            response.setResponseMTI();
            response.set(39, "00");
            sp.out(out, response);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
