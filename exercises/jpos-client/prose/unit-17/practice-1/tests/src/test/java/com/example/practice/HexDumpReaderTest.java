package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

class HexDumpReaderTest {
    /** An 0100 authorization for terminal 29110002, packed with iso87ascii: 81 bytes, six lines. */
    static final byte[] AUTH = ISOUtil.hex2byte(
          "3031303033303230303030303230383038303030303030303030303030303030"
        + "3031323334353030303431373234353535353535353535353535343434343D32"
        + "3731323130313239313130303032393738");

    /** The same 81 bytes as the Unix tool `hexdump -C` prints them. */
    static final String HD = """
        00000000  30 31 30 30 33 30 32 30  30 30 30 30 32 30 38 30  |0100302000002080|
        00000010  38 30 30 30 30 30 30 30  30 30 30 30 30 30 30 30  |8000000000000000|
        00000020  30 31 32 33 34 35 30 30  30 34 31 37 32 34 35 35  |0123450004172455|
        00000030  35 35 35 35 35 35 35 35  35 35 34 34 34 34 3d 32  |55555555554444=2|
        00000040  37 31 32 31 30 31 32 39  31 31 30 30 30 32 39 37  |7121012911000297|
        00000050  38                                                |8|
        00000051
        """;

    private static String without(String dump, String prefix) {
        return dump.lines().filter(l -> !l.startsWith(prefix)).reduce("", (a, b) -> a + b + "\n");
    }

    @Test
    void readsBackWhatHexdumpWrote() {
        assertArrayEquals(AUTH, HexDumpReader.read(ISOUtil.hexdump(AUTH)));
        byte[] sixteen = ISOUtil.hex2byte("F0F1F2F3F4F5F6F7F8F9C1C2C3C4C5C6");
        assertArrayEquals(sixteen, HexDumpReader.read(ISOUtil.hexdump(sixteen)));
        byte[] three = { 0x02, 0x1C, 0x03 };
        assertArrayEquals(three, HexDumpReader.read(ISOUtil.hexdump(three)));
    }

    @Test
    void aReadDumpUnpacksToTheSameMessage() throws Exception {
        GenericPackager p = new GenericPackager("jar:packager/iso87ascii.xml");
        ISOMsg back = new ISOMsg();
        back.setPackager(p);
        assertEquals(AUTH.length, back.unpack(HexDumpReader.read(ISOUtil.hexdump(AUTH))));
        assertEquals("0100", back.getMTI());
        assertEquals("000417", back.getString(11));
        assertEquals("000000012345", back.getString(4));
    }

    @Test
    void blankLinesAndWindowsLineEndingsAreTolerated() {
        String dump = "\r\n" + ISOUtil.hexdump(AUTH).replace("\n", "\r\n") + "\r\n   \r\n";
        assertArrayEquals(AUTH, HexDumpReader.read(dump));
        assertArrayEquals(new byte[0], HexDumpReader.read(""));
    }

    @Test
    void theTextColumnIsNeverReadAsBytes() {
        byte[] looksLikeHex = "7F 0A".getBytes(StandardCharsets.US_ASCII);   // its text column reads "7F 0A"
        assertArrayEquals(looksLikeHex, HexDumpReader.read(ISOUtil.hexdump(looksLikeHex)));
        byte[] fullLine = "PIN 52 MAC 64 OK".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(fullLine, HexDumpReader.read(ISOUtil.hexdump(fullLine)));
    }

    @Test
    void aLostLineIsRefused() {
        String dump = ISOUtil.hexdump(AUTH);
        assertThrows(IllegalArgumentException.class, () -> HexDumpReader.read(without(dump, "0030")));
        String twice = dump.lines().findFirst().get() + "\n" + dump;
        assertThrows(IllegalArgumentException.class, () -> HexDumpReader.read(twice), "a line pasted twice");
    }

    @Test
    void readsTheLayoutOfHexdumpDashC() {
        assertArrayEquals(AUTH, HexDumpReader.read(HD));
        assertThrows(IllegalArgumentException.class, () -> HexDumpReader.read(without(HD, "00000020")));
    }

    @Test
    void lowerCaseHexIsRead() {
        byte[] track = "=2712".getBytes(StandardCharsets.US_ASCII);
        String lower = "0000  3d 32 37 31 32                                    =2712\n";
        assertArrayEquals(track, HexDumpReader.read(lower));
    }

    @Test
    void aDumpCopiedFromALogEventIsRead() {
        String fromLog = "    " + ISOUtil.hexdump(AUTH);         // the writer indents only the first line
        assertArrayEquals(AUTH, HexDumpReader.read(fromLog));
        String allIndented = ISOUtil.hexdump(AUTH).lines().map(l -> "  " + l)
                .reduce("", (a, b) -> a + b + "\n");
        assertArrayEquals(AUTH, HexDumpReader.read(allIndented));
    }

    @Test
    void somethingThatIsNotAByteIsRefused() {
        String dump = ISOUtil.hexdump(AUTH);
        String damaged = dump.substring(0, dump.lastIndexOf("38")) + "3G" + dump.substring(dump.lastIndexOf("38") + 2);
        assertThrows(IllegalArgumentException.class, () -> HexDumpReader.read(damaged), "a byte position holding 3G");
        assertThrows(IllegalArgumentException.class,
            () -> HexDumpReader.read("30 31 30 30 33 30 32 30  30 30 30 30 32 30 38 30  0100302000002080\n"),
            "a line without an offset");
    }
}
