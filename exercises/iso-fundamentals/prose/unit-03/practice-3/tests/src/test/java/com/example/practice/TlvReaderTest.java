package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.practice.TlvReader.Tlv;
import java.util.List;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

class TlvReaderTest {

    private static List<Tlv> read(String hex) {
        return TlvReader.read(ISOUtil.hex2byte(hex));
    }

    @Test
    void splitsAnAuthorizationsChipData() {
        List<Tlv> got = read("9F0206000000007500" + "82021980" + "9F1A020826" + "9A03261106" + "9C0100" + "9F3700");
        assertEquals(List.of(
                new Tlv("9F02", "000000007500"),
                new Tlv("82", "1980"),
                new Tlv("9F1A", "0826"),
                new Tlv("9A", "261106"),
                new Tlv("9C", "00"),
                new Tlv("9F37", "")), got);
    }

    @Test
    void otherFirstBytesStartLongerTagsToo() {
        List<Tlv> got = read("5F2A020978" + "5F340101" + "DF0103ABCDEF" + "95050000048000");
        assertEquals(List.of(
                new Tlv("5F2A", "0978"),
                new Tlv("5F34", "01"),
                new Tlv("DF01", "ABCDEF"),
                new Tlv("95", "0000048000")), got);
    }

    @Test
    void aTagCanHaveThreeBytes() {
        List<Tlv> got = read("DF810103010203" + "9F1A020826");
        assertEquals(List.of(new Tlv("DF8101", "010203"), new Tlv("9F1A", "0826")), got);
    }

    @Test
    void longValuesAreReadWhole() {
        String v144 = "5A".repeat(144);
        String v300 = "C3".repeat(300);
        List<Tlv> got = read("9F1081" + "90" + v144 + "DF02" + "82012C" + v300 + "9C0120");
        assertEquals(List.of(new Tlv("9F10", v144), new Tlv("DF02", v300), new Tlv("9C", "20")), got);
    }

    @Test
    void dataThatEndsTooSoonIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> read("9F0206000000"));
        assertThrows(IllegalArgumentException.class, () -> read("82021980" + "9A0326"));
    }
}
