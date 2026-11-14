import com.example.practice.IssuerResponse;
import com.example.practice.IssuerResponse.Script;
import java.util.List;
import org.jpos.iso.ISOUtil;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main and see what prints. Submit grades. */
public class TryIt {
    /** Held here, so the JDK's own exit notice stays off: a logger nothing holds can be dropped. */
    private static final Logger JDK_EXIT = Logger.getLogger("java.lang.Runtime");

    public static void main(String[] args) {
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
            byte[] arpc = ISOUtil.hex2byte("1122334455667788" + "3030");
            byte[] unblock = ISOUtil.hex2byte("8424000008" + "C1C2C3C4C5C6C7C8");
            byte[] longCommand = new byte[200];
            String shortOne = ISOUtil.hexString(IssuerResponse.de55(arpc, List.of(
                    new Script(0x71, ISOUtil.hex2byte("0000B001"), List.of(unblock)))));
            System.out.println("with tag 91 and one script: " + shortOne);
            String noId = ISOUtil.hexString(IssuerResponse.de55(null, List.of(new Script(0x72, null, List.of(unblock)))));
            System.out.println("script without identifier: " + noId);
            byte[] big = IssuerResponse.de55(null, List.of(new Script(0x72, ISOUtil.hex2byte("0000B002"), List.of(longCommand))));
            System.out.println("long script starts: " + ISOUtil.hexString(big).substring(0, 26));
            System.out.println("long script bytes: " + big.length);
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
