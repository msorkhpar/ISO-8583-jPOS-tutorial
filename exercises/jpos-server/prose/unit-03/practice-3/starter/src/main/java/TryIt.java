import com.example.practice.TerminalDirectory;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;

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
            TerminalDirectory dir = new TerminalDirectory();
            Connection till = new Connection("till-A");
            System.out.println("passed on: " + !dir.process(till, message("57300012", "001204", "301")));
            System.out.println("sign-off sent: " + dir.send(message("57300012", "770031", "002")));
            System.out.println("unknown terminal sent: " + dir.send(message("57300099", "770032", "002")));
            till.connected = false;
            System.out.println("after switch-off sent: " + dir.send(message("57300012", "770033", "002")));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static final class Connection implements ISOSource {
        private final String name;
        boolean connected = true;

        Connection(String name) {
            this.name = name;
        }

        @Override
        public void send(ISOMsg m) {
            System.out.println(name + " got: " + m.getString(0) + " field 70=" + m.getString(70) + " for " + m.getString(41));
        }

        @Override
        public boolean isConnected() {
            return connected;
        }
    }

    private static ISOMsg message(String terminal, String stan, String code) throws Exception {
        ISOMsg m = new ISOMsg("0800");
        m.set(7, "1114181500");
        m.set(11, stan);
        m.set(41, terminal);
        m.set(70, code);
        return m;
    }
}
