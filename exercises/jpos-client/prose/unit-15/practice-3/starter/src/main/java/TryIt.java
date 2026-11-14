import com.example.practice.LinkStop;
import org.jdom2.Element;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.q2.iso.QMUX;
import org.jpos.space.LocalSpace;
import org.jpos.space.SpaceFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
            @SuppressWarnings("unchecked")
            LocalSpace<String, Object> sp = (LocalSpace<String, Object>) SpaceFactory.getSpace("tspace:tryit");
            sp.addListener("east-send", (q, v) -> {
                try { ISOMsg m = (ISOMsg) sp.inp(q); if (m != null) receive(sp, m); } catch (Exception e) { throw new IllegalStateException(e); }
            });
            Element e = new Element("mux");
            e.addContent(new Element("space").setText("tspace:tryit"));
            e.addContent(new Element("in").setText("east-receive"));
            e.addContent(new Element("out").setText("east-send"));
            e.addContent(new Element("ready").setText("east-channel.ready"));
            QMUX mux = new QMUX();
            mux.setName("east-mux");
            mux.setPersist(e);
            mux.setConfiguration(new SimpleConfiguration());
            mux.init();
            mux.start();
            try {
                Date ready = new Date();
                sp.put("east-channel.ready", ready);
                sp.put("east-mux.logon", ready);
                sp.put("east-mux.enabled", ready);
                ISOMsg p = new ISOMsg("0200");
                p.set(3, "000000"); p.set(4, "000000006480"); p.set(11, "003301"); p.set(41, "27180005");
                CompletableFuture<ISOMsg> purchase = CompletableFuture.supplyAsync(() -> {
                    try { return mux.request(p, 5000); } catch (Exception x) { throw new IllegalStateException(x); }
                });
                while (mux.getRXPending() == 0) Thread.onSpinWait();
                int[] pauses = { 0 };
                LinkStop stop = new LinkStop(sp, mux, "east-mux", "east-channel.ready", () -> {
                    if (++pauses[0] == 2) { synchronized (TryIt.class) { for (ISOMsg m : held) answer(sp, m); held.clear(); } }
                    else Thread.sleep(10);
                }, 50);
                System.out.println("still waiting at sign-off: " + stop.stop());
                    System.out.println("purchase answer: " + purchase.get().getString(39));
                System.out.println("host saw, in order: " + seen);
                System.out.println("enabled mark left: " + (sp.rdp("east-mux.enabled") != null));
            } finally {
                mux.stop();
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static final List<String> seen = new ArrayList<>();
    private static final List<ISOMsg> held = new ArrayList<>();

    /** The host holds purchases until told to answer, and answers network messages at once. */
    private static synchronized void receive(LocalSpace<String, Object> sp, ISOMsg m) throws Exception {
        seen.add(m.getMTI() + (m.hasField(70) ? "/" + m.getString(70) : ""));
        if (m.getMTI().equals("0200")) held.add(m); else answer(sp, m);
    }

    private static void answer(LocalSpace<String, Object> sp, ISOMsg m) {
        ISOMsg r = (ISOMsg) m.clone();
        try { r.setResponseMTI(); r.set(39, "00"); } catch (Exception e) { throw new IllegalStateException(e); }
        sp.out("east-receive", r);
    }
}
