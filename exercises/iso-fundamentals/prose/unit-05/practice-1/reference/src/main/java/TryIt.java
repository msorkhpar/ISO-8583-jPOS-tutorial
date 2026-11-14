import com.example.practice.Base1Frames;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.Base1Packager;

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
            byte[] first = message("0110", "300401");
            byte[] second = message("0410", "300402");
            ByteArrayOutputStream link = new ByteArrayOutputStream();
            link.writeBytes(frame(first));
            link.writeBytes(new byte[4]);
            link.writeBytes(frame(second));
            ByteArrayOutputStream back = new ByteArrayOutputStream();
            Base1Frames frames = new Base1Frames(new ByteArrayInputStream(link.toByteArray()), back);
            for (Base1Frames.Frame f = frames.next(); f != null; f = frames.next()) {
                ISOMsg m = new ISOMsg();
                m.setPackager(new Base1Packager());
                m.unpack(f.message());
                System.out.println("frame: header " + f.header().length + " bytes, " + m.getMTI() + " stan " + m.getString(11));
            }
            System.out.println("bytes sent back to the network: " + back.size());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static byte[] message(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.setPackager(new Base1Packager());
        m.set(3, "000000");
        m.set(4, "000000007300");
        m.set(11, stan);
        m.set(39, "00");
        m.set(41, "TERM0500");
        return m.pack();
    }

    private static byte[] frame(byte[] message) {
        int len = 22 + message.length;
        byte[] header = new byte[22];
        header[0] = 22;
        header[1] = 0x01;
        header[3] = (byte) (len >> 8);
        header[4] = (byte) len;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] { (byte) (len >> 8), (byte) len, 0, 0 });
        out.writeBytes(header);
        out.writeBytes(message);
        return out.toByteArray();
    }
}
