import com.example.practice.EchoProbe;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.Deque;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.ASCIIChannel;
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
            Scripted up = new Scripted().frame(25, msg("0810", "770101"));
            System.out.println("host answers: " + new EchoProbe(up, up::now).check("770101", 4000));
            Scripted other = new Scripted().frame(20, msg("0810", "770099")).frame(-1, null);
            System.out.println("only a stale answer: " + new EchoProbe(other, other::now).check("770102", 4000));
            Scripted gone = new Scripted().frame(10, "hang-up");
            System.out.println("host hangs up: " + new EchoProbe(gone, gone::now).check("770103", 4000));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg msg(String mti, String stan) {
        ISOMsg m = new ISOMsg(mti);
        m.set(11, stan);
        m.set(70, "301");
        return m;
    }

    /** A channel with no socket: each receive plays the next step; a null step is a read timeout. */
    private static final class Scripted extends ASCIIChannel {
        private final Deque<Object[]> script = new ArrayDeque<>();
        private long now = 50_000;

        Scripted frame(long at, Object step) { script.add(new Object[] { at, step }); return this; }
        long now() { return now; }
        @Override public boolean isConnected() { return true; }
        @Override public void send(ISOMsg m) { }

        @Override public ISOMsg receive() throws IOException {
            Object[] step = script.poll();
            if (step == null || step[1] == null) { now += 3000; throw new SocketTimeoutException("Read timed out"); }
            now = 50_000 + (Long) step[0];
            if ("hang-up".equals(step[1])) throw new EOFException();
            return (ISOMsg) step[1];
        }
    }
}
