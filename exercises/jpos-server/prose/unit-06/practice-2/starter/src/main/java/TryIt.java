import com.example.practice.TransmissionWindow;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;

import java.util.Date;
import java.util.TimeZone;
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
            TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
            TransmissionWindow window = new TransmissionWindow();
            System.out.println("sent 2 minutes ago: " + check(window, -2));
            System.out.println("sent 4 minutes ahead: " + check(window, 4));
            System.out.println("sent 9 minutes ago: " + check(window, -9));
            System.out.println("sent 3 hours ago: " + check(window, -180));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static String check(TransmissionWindow window, int minutes) throws Exception {
        String field7 = ISODate.getDateTime(new Date(System.currentTimeMillis() + minutes * 60_000L),
                TimeZone.getTimeZone("GMT"));
        ISOMsg m = new ISOMsg("0100");
        m.set(3, "000000");
        m.set(4, "000000001990");
        m.set(7, field7);
        m.set(11, "002741");
        m.set(41, "60400063");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        window.prepare(1L, ctx);
        ISOMsg response = ctx.get(ContextConstants.RESPONSE.toString());
        return "field 7=" + field7 + ", SENT_AT=" + ctx.get("SENT_AT")
                + (response == null ? ", passes" : ", answered " + response.getString(39));
    }
}
