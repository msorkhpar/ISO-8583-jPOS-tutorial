import com.example.practice.EvidenceKeeper;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.Dumpable;
import org.jpos.util.LogEvent;
import org.jpos.util.SimpleLogSource;
import java.util.Arrays;
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
            ISOMsg reply = new ISOMsg("0210");
            reply.set(3, "000000");
            reply.set(4, "000000005060");
            reply.set(11, "000712");
            reply.set(39, "00");
            reply.set(41, "27180040");
            reply.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
            byte[] wire = reply.pack();
            byte[] cut = Arrays.copyOf(wire, wire.length - 3);
            LogEvent ev = new LogEvent(new SimpleLogSource(new org.jpos.util.Logger(), "east-channel/10.0.0.5:9000"), "receive");
            ev.addMessage(new ISOException("org.jpos.iso.IF_CHAR: Problem unpacking field 41"));
            ev.addMessage("--- header ---");
            ev.addMessage(ISOUtil.hexdump(ISOUtil.hex2byte("6000120000")));
            ev.addMessage("--- data ---");
            ev.addMessage(ISOUtil.hexdump(cut));
            ev.addMessage(new Dumpable("image", wire));
            new EvidenceKeeper((realm, part, bytes) ->
                    System.out.println("store got " + part + " from " + realm + ": " + ISOUtil.hexString(bytes)))
                    .log(ev);
            for (Object entry : ev.getPayLoad())
                System.out.println("log shows: " + entry);
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
