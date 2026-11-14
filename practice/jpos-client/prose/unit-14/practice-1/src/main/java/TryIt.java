import com.example.practice.PoolMonitor;
import org.jdom2.Element;
import org.jpos.core.SimpleConfiguration;
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
            QMUX north = mux("north-mux", "north-link");
            QMUX south = mux("south-mux", "south-link");
            try {
                PoolMonitor monitor = new PoolMonitor(sp, true, north, south);
                System.out.println("both down: " + monitor.skipped());
                Date n1 = new Date(1_763_200_000_000L);
                Date s1 = new Date(1_763_200_003_000L);
                sp.put("north-link.ready", n1);
                sp.put("south-link.ready", s1);
                System.out.println("connected, no sign-on: " + monitor.skipped());
                sp.put("north-mux.enabled", n1);
                sp.put("south-mux.enabled", s1);
                System.out.println("both signed on: " + monitor.skipped());
                sp.put("south-link.ready", new Date(1_763_200_060_000L));
                System.out.println("south reconnected: " + monitor.skipped());
                System.out.println("without check-enabled: " + new PoolMonitor(sp, false, north, south).skipped());
            } finally {
                north.stop();
                south.stop();
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static QMUX mux(String name, String link) throws Exception {
        Element e = new Element("mux");
        e.addContent(new Element("space").setText("tspace:tryit"));
        e.addContent(new Element("in").setText(link + "-receive"));
        e.addContent(new Element("out").setText(link + "-send"));
        e.addContent(new Element("ready").setText(link + ".ready"));
        QMUX q = new QMUX();
        q.setName(name);
        q.setPersist(e);
        q.setConfiguration(new SimpleConfiguration());
        q.init();
        q.start();
        return q;
    }
}
