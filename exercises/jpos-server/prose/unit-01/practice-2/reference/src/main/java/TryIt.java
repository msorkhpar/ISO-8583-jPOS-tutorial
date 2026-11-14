import com.example.practice.ApprovalJournal;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
import org.jpos.util.NameRegistrar;
import java.util.ArrayList;
import java.util.List;
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
            List<String> lines = new ArrayList<>();
            NameRegistrar.register("journal", (ApprovalJournal.Lines) (t, s, a) -> lines.add(t + " " + s + " " + a));
            try {
                ApprovalJournal journal = new ApprovalJournal();
                journal.setConfiguration(new SimpleConfiguration());
                run(journal, "0200", "000701", "00", false);
                System.out.println("lines after approved purchase: " + lines.size());
                run(journal, "0200", "000702", "51", false);
                System.out.println("lines after declined purchase: " + lines.size());
                run(journal, "0200", "000703", "00", true);
                System.out.println("lines after approved, aborted later: " + lines.size());
                run(journal, "0100", "000704", "00", false);
                System.out.println("lines after approved authorization: " + lines.size());
                System.out.println("journal: " + lines);
            } finally {
                NameRegistrar.unregister("journal");
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    /** One transaction through the journal, ending in commit or, when a later participant fails, abort. */
    private static void run(ApprovalJournal journal, String mti, String stan, String rc, boolean laterFails) throws Exception {
        ISOMsg request = new ISOMsg(mti);
        request.set(4, "000000003675");
        request.set(11, stan);
        request.set(41, "27180060");
        ISOMsg response = (ISOMsg) request.clone();
        response.setResponseMTI();
        response.set(39, rc);
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), request);
        ctx.put(ContextConstants.RESPONSE.toString(), response);
        int result = journal.prepare(1L, ctx);
        if ((result & TransactionConstants.NO_JOIN) != 0)
            return;
        if (laterFails) journal.abort(1L, ctx); else journal.commit(1L, ctx);
    }
}
