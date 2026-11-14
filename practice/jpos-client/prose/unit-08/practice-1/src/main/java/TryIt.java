import com.example.practice.EchoResponder;
import java.io.IOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.util.SimpleLogListener;
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
            org.jpos.util.Logger node = new org.jpos.util.Logger();
            node.addListener(new SimpleLogListener(System.out));
            EchoResponder responder = new EchoResponder();
            responder.setLogger(node, "echo-responder");
            ISOMsg echo = new ISOMsg("0800");
            echo.set(7, "1114103000");
            echo.set(11, "120044");
            echo.set(70, "301");
            System.out.println("handled echo: " + responder.process(new Link(false), echo));
            echo.set(11, "120045");
            System.out.println("handled echo on a broken link: " + responder.process(new Link(true), echo));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    /** The host's side of the link: prints each reply, or fails to send. */
    private record Link(boolean broken) implements ISOSource {
        @Override public void send(ISOMsg m) throws IOException {
            if (broken) {
                IOException reset = new IOException("broken pipe");
                reset.setStackTrace(new StackTraceElement[0]);
                throw reset;
            }
            System.out.println("reply sent: " + m.getString(0) + " STAN " + m.getString(11) + " rc " + m.getString(39));
        }
        @Override public boolean isConnected() { return true; }
    }
}
