import com.example.practice.Submitter;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jdom2.Element;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
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
            Element e = new Element("mux");
            e.addContent(new Element("space").setText("tspace:tryit"));
            e.addContent(new Element("in").setText("host-receive"));
            e.addContent(new Element("out").setText("host-send"));
            e.addContent(new Element("ready").setText("host.ready"));
            QMUX mux = new QMUX();
            mux.setName("shop-mux");
            mux.setPersist(e);
            mux.setConfiguration(new SimpleConfiguration());
            AtomicBoolean silent = new AtomicBoolean();
            try {
                mux.init();
                mux.start();
                sp.out("host.ready", Boolean.TRUE);
                sp.addListener("host-send", (key, value) -> {
                    ISOMsg request = (ISOMsg) sp.inp(key);
                    if (request != null && !silent.get())
                        sp.out("host-receive", answer(request));
                });
                Submitter shop = new Submitter(mux, new GenericPackager("jar:packager/iso87ascii.xml"), 500);
                Submitter.Result ok = shop.submit(purchase("000881", "CORNER BOOKS            LYON         FR"));
                System.out.println("fits: " + ok.outcome() + ", answer " + (ok.response() == null ? null : ok.response().getMTI()));
                Submitter.Result tooLong = shop.submit(purchase("000882", "CORNER BOOKS AND STATIONERY   LYON CEDEX FR"));
                System.out.println("field 43 too long: " + tooLong.outcome() + ", reason: " + tooLong.reason());
                silent.set(true);
                System.out.println("host silent: " + shop.submit(purchase("000883", "CORNER BOOKS")).outcome());
            } finally {
                mux.stop();
                mux.destroy();
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String stan, String nameAndLocation) {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4000056655665556");
        m.set(3, "000000");
        m.set(4, "000000002390");
        m.set(7, "0602113005");
        m.set(11, stan);
        m.set(41, "40100022");
        m.set(43, nameAndLocation);
        m.set(49, "978");
        return m;
    }

    private static ISOMsg answer(ISOMsg request) {
        try {
            ISOMsg response = (ISOMsg) request.clone();
            response.setResponseMTI();
            response.set(39, "00");
            return response;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
