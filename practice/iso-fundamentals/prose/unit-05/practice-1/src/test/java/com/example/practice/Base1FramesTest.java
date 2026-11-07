package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.practice.Base1Frames.Frame;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.Base1Packager;
import org.junit.jupiter.api.Test;

class Base1FramesTest {

    /** A message as Base1Packager writes it. */
    private static byte[] message(String mti, String stan, String responseCode) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.setPackager(new Base1Packager());
        m.set(3, "000000");
        m.set(4, "000000002599");
        m.set(11, stan);
        if (responseCode != null)
            m.set(39, responseCode);
        m.set(41, "29110001");
        return m.pack();
    }

    /** A 22-byte header in front of a message of {@code messageLength} bytes. */
    private static byte[] header(int messageLength, String destination, String source) {
        byte[] h = new byte[22];
        h[0] = 22;
        h[1] = 0x01;
        h[2] = 0x02;
        int total = 22 + messageLength;
        h[3] = (byte) (total >> 8);
        h[4] = (byte) total;
        System.arraycopy(ISOUtil.str2bcd(destination, true), 0, h, 5, 3);
        System.arraycopy(ISOUtil.str2bcd(source, true), 0, h, 8, 3);
        return h;
    }

    /** A 26-byte reject header, to go in front of the original header. */
    private static byte[] rejectHeader(byte[] original, String rejectCode) {
        byte[] r = new byte[26];
        System.arraycopy(original, 0, r, 0, 22);
        r[0] = 26;
        r[1] = (byte) 0x81;
        r[22] = (byte) 0x80;
        byte[] code = ISOUtil.str2bcd(rejectCode, true);
        r[24] = code[0];
        r[25] = code[1];
        return r;
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts)
            out.writeBytes(p);
        return out.toByteArray();
    }

    /** The prefix and everything behind it. */
    private static byte[] frame(byte[] headers, byte[] message) {
        int len = headers.length + message.length;
        return concat(new byte[] { (byte) (len >> 8), (byte) len, 0, 0 }, headers, message);
    }

    @Test
    void splitsOneFrame() throws Exception {
        byte[] msg = message("0110", "004711", "00");
        byte[] hdr = header(msg.length, "400001", "700007");
        Base1Frames frames = new Base1Frames(new ByteArrayInputStream(frame(hdr, msg)), new ByteArrayOutputStream());

        assertEquals(new Frame(hdr, msg), frames.next());
        assertNull(frames.next());
    }

    @Test
    void splitsFramesBackToBack() throws Exception {
        byte[] first = message("0110", "004712", "05");
        byte[] second = message("0410", "004713", "00");
        byte[] h1 = header(first.length, "400001", "700007");
        byte[] h2 = header(second.length, "400001", "700007");
        Base1Frames frames = new Base1Frames(
                new ByteArrayInputStream(concat(frame(h1, first), frame(h2, second))), new ByteArrayOutputStream());

        assertEquals(new Frame(h1, first), frames.next());
        assertEquals(new Frame(h2, second), frames.next());
        assertNull(frames.next());
    }

    @Test
    void answersAPoll() throws Exception {
        byte[] msg = message("0110", "004714", "00");
        byte[] hdr = header(msg.length, "400001", "700007");
        ByteArrayOutputStream back = new ByteArrayOutputStream();
        Base1Frames frames = new Base1Frames(
                new ByteArrayInputStream(concat(new byte[4], frame(hdr, msg), new byte[4])), back);

        assertEquals(new Frame(hdr, msg), frames.next());
        assertNull(frames.next());
        assertArrayEquals(new byte[8], back.toByteArray(), "each poll goes back to the network as it came");
    }

    @Test
    void readsARejectHeaderWhole() throws Exception {
        byte[] msg = message("0100", "004715", null);
        byte[] original = header(msg.length, "700007", "400001");
        byte[] headers = concat(rejectHeader(original, "0423"), original);
        Base1Frames frames = new Base1Frames(new ByteArrayInputStream(frame(headers, msg)), new ByteArrayOutputStream());

        assertEquals(new Frame(headers, msg), frames.next());
    }

    @Test
    void theNextFrameStartsWhereARejectEnds() throws Exception {
        byte[] rejected = message("0100", "004716", null);
        byte[] original = header(rejected.length, "700007", "400001");
        byte[] answer = message("0110", "004717", "00");
        byte[] h2 = header(answer.length, "400001", "700007");
        Base1Frames frames = new Base1Frames(new ByteArrayInputStream(concat(
                frame(concat(rejectHeader(original, "0388"), original), rejected), frame(h2, answer))),
                new ByteArrayOutputStream());

        frames.next();
        assertEquals(new Frame(h2, answer), frames.next());
        assertNull(frames.next());
    }
}
