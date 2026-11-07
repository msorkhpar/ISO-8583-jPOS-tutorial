package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrameReaderTest {

    /** A packed 0100 of 70 bytes, built fresh for each use. */
    private static byte[] authorization(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000001999");
        m.set(11, stan);
        m.set(41, "29110001");
        m.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        return m.pack();
    }

    private static byte[] bytes(int n, int seed) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++)
            b[i] = (byte) (seed + i * 7);
        return b;
    }

    /** The frames as the host writes them: two length bytes, high byte first, then the message. */
    private static byte[] wire(byte[]... messages) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] m : messages) {
            out.write(m.length >> 8);
            out.write(m.length & 0xFF);
            out.write(m, 0, m.length);
        }
        return out.toByteArray();
    }

    /** A stream that hands out at most `step` bytes per read, as a slow network does. */
    private static final class Trickle extends InputStream {
        private final byte[] data;
        private final int step;
        private int pos;

        Trickle(byte[] data, int step) {
            this.data = data;
            this.step = step;
        }

        @Override
        public int read() {
            return pos < data.length ? data[pos++] & 0xFF : -1;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (len == 0) return 0;
            if (pos >= data.length) return -1;
            int n = Math.min(Math.min(len, step), data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }
    }

    @Test
    void aMessageIsReturnedWithoutItsLength() throws Exception {
        byte[] auth = authorization("004417");
        byte[] got = new FrameReader(new ByteArrayInputStream(wire(auth))).next();
        assertArrayEquals(authorization("004417"), got);
        ISOMsg m = new ISOMsg();
        m.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        m.unpack(got);
        assertEquals("004417", m.getString(11));
    }

    @Test
    void aLongMessageIsReturnedWhole() throws Exception {
        byte[] got = new FrameReader(new ByteArrayInputStream(wire(bytes(300, 3)))).next();
        assertArrayEquals(bytes(300, 3), got);
    }

    @Test
    void aMessageThatArrivesInPiecesIsReturnedWhole() throws Exception {
        FrameReader reader = new FrameReader(new Trickle(wire(authorization("004418")), 3));
        assertArrayEquals(authorization("004418"), reader.next());
    }

    @Test
    void twoMessagesThatArriveTogetherAreReturnedOneAtATime() throws Exception {
        FrameReader reader = new FrameReader(
                new ByteArrayInputStream(wire(authorization("004419"), authorization("004420"))));
        assertArrayEquals(authorization("004419"), reader.next());
        assertArrayEquals(authorization("004420"), reader.next());
    }

    @Test
    void aMessageOf200BytesIsReturnedWhole() throws Exception {
        byte[] got = new FrameReader(new ByteArrayInputStream(wire(bytes(200, 5)))).next();
        assertArrayEquals(bytes(200, 5), got);
    }

    @Test
    void aStreamThatEndsInsideAMessageIsAnError() {
        byte[] full = wire(bytes(80, 9));
        byte[] cut = java.util.Arrays.copyOf(full, 2 + 30);       // the connection drops after 30 bytes
        FrameReader reader = new FrameReader(new ByteArrayInputStream(cut));
        assertThrows(EOFException.class, reader::next);
    }

    @Test
    void anIdleHostsEmptyMessagesAreSkipped() throws Exception {
        FrameReader reader = new FrameReader(
                new ByteArrayInputStream(wire(new byte[0], new byte[0], authorization("004421"))));
        assertArrayEquals(authorization("004421"), reader.next());
    }
}
