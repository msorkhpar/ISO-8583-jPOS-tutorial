package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.practice.CompositeWriter.Dataset;
import com.example.practice.CompositeWriter.Element;
import java.util.Arrays;
import java.util.List;
import org.jpos.iso.ISOUtil;
import org.jpos.tlv.TLVList;
import org.junit.jupiter.api.Test;

class CompositeWriterTest {

    private static Element el(int tag, String hex) {
        return new Element(tag, ISOUtil.hex2byte(hex));
    }

    /** {@code n} bytes 01 02 03 ... wrapping after FF. */
    private static byte[] bytes(int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++)
            b[i] = (byte) (i + 1);
        return b;
    }

    private static String hex(byte[] b) {
        return ISOUtil.hexString(b);
    }

    private static byte[] one(int id, Element... elements) {
        return CompositeWriter.write(List.of(new Dataset(id, List.of(elements))));
    }

    /** The elements of the first dataset, read back by jPOS. */
    private static TLVList readBack(byte[] field) {
        TLVList tlv = new TLVList();
        tlv.unpack(Arrays.copyOfRange(field, 3, field.length));
        return tlv;
    }

    @Test
    void writesDatasetsInTheirEnvelopes() {
        assertEquals("0400048A023030", hex(one(0x04, el(0x8A, "3030"))));

        // Two datasets in the order given; one-, two- and three-byte tags; an empty dataset
        byte[] field = CompositeWriter.write(List.of(
                new Dataset(0x22, List.of(el(0x9F03, "000000000150"), el(0x9C, "20"))),
                new Dataset(0x0E, List.of()),
                new Dataset(0x03, List.of(el(0xDF8117, "E0"), el(0xDF20, "")))));
        assertEquals("22000C" + "9F0306000000000150" + "9C0120"
                + "0E0000"
                + "030008" + "DF811701E0" + "DF2000", hex(field));

        assertEquals("", hex(CompositeWriter.write(List.of())));
    }

    @Test
    void refusesIdentifiersOutsideTheTlvRange() {
        for (int id : new int[] { 0x00, 0x9B, 0xFE, 0xFF }) {
            List<Dataset> one = List.of(new Dataset(id, List.of(el(0x8A, "3035"))));
            assertThrows(IllegalArgumentException.class, () -> CompositeWriter.write(one), "id " + id);
            List<Dataset> empty = List.of(new Dataset(id, List.of()));
            assertThrows(IllegalArgumentException.class, () -> CompositeWriter.write(empty), "empty, id " + id);
        }
    }

    @Test
    void valuesFrom128BytesUseTheLongLengthForm() {
        byte[] f = one(0x02, new Element(0xDF21, bytes(160)));
        assertEquals("0200A4" + "DF2181A0", hex(f).substring(0, 14));
        assertEquals(3 + 4 + 160, f.length);
        assertEquals(hex(bytes(160)), readBack(f).getString(0xDF21));

        f = one(0x02, new Element(0xDF21, bytes(127)));
        assertEquals("020082" + "DF217F", hex(f).substring(0, 12));
        f = one(0x02, new Element(0xDF21, bytes(128)));
        assertEquals("020084" + "DF218180", hex(f).substring(0, 14));
        assertEquals(hex(bytes(128)), readBack(f).getString(0xDF21));
    }

    @Test
    void datasetsOver255BytesKeepTheirWholeLength() {
        // 2 + 1 + 90 bytes, three times: 279 bytes of content = 0x0117
        Element e = new Element(0xDF22, bytes(90));
        byte[] f = one(0x0D, e, e, e);
        assertEquals("0D0117", hex(f).substring(0, 6));
        assertEquals(3 + 279, f.length);
    }

    @Test
    void dataset0x70IsATlvDataset() {
        assertEquals("7000039C0109", hex(one(0x70, el(0x9C, "09"))));
        List<Dataset> bitmap = List.of(new Dataset(0x71, List.of(el(0x9C, "09"))));
        assertThrows(IllegalArgumentException.class, () -> CompositeWriter.write(bitmap));
    }

    @Test
    void writesDatasetsOfAFewHundredBytes() {
        Element a = new Element(0xDF23, bytes(250));
        Element b = new Element(0xDF24, bytes(350));
        byte[] f = one(0x05, a, b);
        int content = (2 + 2 + 250) + (2 + 3 + 350);
        assertEquals(String.format("05%04X", content), hex(f).substring(0, 6));
        assertEquals(3 + content, f.length);
        assertEquals(hex(bytes(350)), readBack(f).getString(0xDF24));
    }

    @Test
    void refusesContentTheLengthCannotDescribe() {
        // DF25 + 82 xx xx + value: 2 + 3 + 65530 = 65535 bytes fits exactly
        byte[] f = one(0x06, new Element(0xDF25, new byte[65530]));
        assertEquals("06FFFF", hex(f).substring(0, 6));
        assertEquals(3 + 65535, f.length);

        List<Dataset> tooBig = List.of(new Dataset(0x06, List.of(new Element(0xDF25, new byte[65531]))));
        assertThrows(IllegalArgumentException.class, () -> CompositeWriter.write(tooBig));
    }
}
