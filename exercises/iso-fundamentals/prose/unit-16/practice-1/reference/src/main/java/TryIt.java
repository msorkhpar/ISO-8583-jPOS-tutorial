import com.example.practice.JsonPackager;
import java.nio.charset.StandardCharsets;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

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
            ISOMsg sent = new ISOMsg("0100");
            sent.set(2, "4012888888881881");
            sent.set(4, "000000000505");
            sent.set(11, "016001");
            sent.set(43, "JOE's \"CORNER\" DELI\\BAR   SPRINGFIELD  US");
            sent.set(52, ISOUtil.hex2byte("00A1B2C3D4E5F607"));
            sent.setPackager(new JsonPackager());
            byte[] json = sent.pack();
            System.out.println("json: " + new String(json, StandardCharsets.UTF_8));

            ISOMsg back = new ISOMsg();
            back.setPackager(new JsonPackager());
            back.unpack(json);
            System.out.println("back: " + back.getMTI() + " 4=" + back.getString(4) + " 11=" + back.getString(11));
            System.out.println("43 back: " + back.getString(43));
            System.out.println("52 back: " + ISOUtil.hexString(back.getBytes(52)));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
