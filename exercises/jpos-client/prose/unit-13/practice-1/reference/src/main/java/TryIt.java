import com.example.practice.SwitchPackager;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
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
            SwitchPackager packager = new SwitchPackager();
            ISOMsg echo = new ISOMsg("0800");
            echo.setPackager(packager);
            echo.set(11, "000777");
            echo.set(41, "TERM0042");
            echo.set(70, "301");
            byte[] bytes = echo.pack();
            System.out.println("echo packed: " + ISOUtil.hexString(bytes));
            ISOMsg back = new ISOMsg();
            back.setPackager(packager);
            System.out.println("bytes read: " + back.unpack(bytes) + " of " + bytes.length);
            System.out.println("41: " + back.getString(41) + ", 70: " + back.getString(70));

            ISOMsg purchase = new ISOMsg("0200");
            purchase.setPackager(packager);
            purchase.set(2, "4000056655665556");
            purchase.set(4, "000000002075");
            purchase.set(11, "000778");
            purchase.set(52, ISOUtil.hex2byte("0123456789ABCDEF"));
            System.out.println("purchase packed: " + ISOUtil.hexString(purchase.pack()));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }
}
