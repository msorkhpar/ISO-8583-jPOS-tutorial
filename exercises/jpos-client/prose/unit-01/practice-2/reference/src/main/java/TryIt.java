import com.example.practice.LinkStatus;
import java.util.concurrent.atomic.AtomicBoolean;
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
            LinkStatus status = new LinkStatus("discover-mux", 400);
            System.out.println("before deploy: " + status.check());
            Element e = new Element("mux");
            e.addContent(new Element("space").setText("tspace:tryit"));
            e.addContent(new Element("in").setText("discover-receive"));
            e.addContent(new Element("out").setText("discover-send"));
            e.addContent(new Element("ready").setText("discover-channel.ready"));
            QMUX q = new QMUX();
            q.setName("discover-mux");
            q.setPersist(e);
            q.setConfiguration(new SimpleConfiguration());
            AtomicBoolean silent = new AtomicBoolean();
            try {
                q.init();
                q.start();
                System.out.println("deployed, link down: " + status.check());
                sp.addListener("discover-send", (key, value) -> {
                    ISOMsg request = (ISOMsg) sp.inp(key);
                    if (request != null && !silent.get())
                        sp.out("discover-receive", answer(request));
                });
                sp.out("discover-channel.ready", new java.util.Date());
                System.out.println("link up, host answers: " + status.check());
                silent.set(true);
                System.out.println("link up, host silent: " + status.check());
            } finally {
                q.stop();
                q.destroy();
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
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
