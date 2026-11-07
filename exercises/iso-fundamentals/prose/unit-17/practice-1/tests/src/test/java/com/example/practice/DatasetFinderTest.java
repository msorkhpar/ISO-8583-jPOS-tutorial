package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

class DatasetFinderTest {

    private static ISOMsg message(int field, String hex) throws Exception {
        ISOMsg m = new ISOMsg("2100");
        m.set(11, "000000004417");
        m.set(field, ISOUtil.hex2byte(hex));
        return m;
    }

    // DE 104: TLV dataset 0x03 (DF10 = 2A, 9F1A = 0826), bitmap dataset 0x72, TLV dataset 0x0B.
    private static final String DE104 = "030009" + "DF10012A" + "9F1A020826"
            + "720003" + "A00031"
            + "0B0004" + "DF110107";

    @Test
    void findsDatasetsAndTheirElements() throws Exception {
        ISOMsg m = message(104, DE104);
        assertEquals("2A", DatasetFinder.find(m, "104.0x03.0xDF10"));
        assertEquals("0826", DatasetFinder.find(m, "104.0x03.0x9F1A"));
        assertEquals("DF10012A9F1A020826", DatasetFinder.find(m, "104.0x03"));
        assertEquals("A00031", DatasetFinder.find(m, "104.0x72"));
        assertEquals("07", DatasetFinder.find(m, "104.0x0B.0xDF11"));
        assertNull(DatasetFinder.find(m, "104.0x05"), "a dataset that is not there");
        assertNull(DatasetFinder.find(m, "104.0x03.0xDF12"), "a tag that is not there");
        assertNull(DatasetFinder.find(m, "43.0x03"), "a field that is not there");
        assertNull(DatasetFinder.find(m, "43.0x03.0xDF10"), "a field that is not there");

        // An empty dataset is there, with no content
        ISOMsg empty = message(43, "060000" + "020004" + "DF130199");
        assertEquals("", DatasetFinder.find(empty, "43.0x06"));
        assertEquals("99", DatasetFinder.find(empty, "43.0x02.0xDF13"));
    }

    @Test
    void refusesPathsItCannotResolve() throws Exception {
        ISOMsg m = message(104, DE104);
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(m, "104"));
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(m, "104.0x03.0xDF10.0x01"));
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(m, "104.03"));
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(m, "104.0x72.0x01"));
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(m, "104.0xFE.0x01"));
    }

    @Test
    void chipDataHasNoDatasetLevel() throws Exception {
        ISOMsg m = message(55, "9F2701" + "80" + "82025C00" + "9F3704" + "0BADF00D");
        assertEquals("80", DatasetFinder.find(m, "55.0x9F27"));
        assertEquals("5C00", DatasetFinder.find(m, "55.0x82"));
        assertEquals("0BADF00D", DatasetFinder.find(m, "55.0x9F37"));
        assertNull(DatasetFinder.find(m, "55.0x9F26"));
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(m, "55.0x9F.0x27"));
    }

    @Test
    void tagsInsideDataset0x70() throws Exception {
        ISOMsg m = message(34, "700004" + "DF140142" + "710002" + "8000");
        assertEquals("42", DatasetFinder.find(m, "34.0x70.0xDF14"));
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(m, "34.0x71.0xDF14"));
    }

    @Test
    void identifierBytesInsideAnotherDatasetAreNotDatasets() throws Exception {
        // Dataset 0x0C holds DF15 = 04 00 02 33 44, which looks like a dataset 0x04 of two bytes.
        // Dataset 0x7A is stepped over by its length.
        ISOMsg m = message(34, "0C0008" + "DF150504000233" + "44"
                + "7A0003" + "040001"
                + "040004" + "DF160111");
        assertEquals("DF160111", DatasetFinder.find(m, "34.0x04"));
        assertEquals("11", DatasetFinder.find(m, "34.0x04.0xDF16"));
        assertEquals("040001", DatasetFinder.find(m, "34.0x7A"));
    }

    @Test
    void refusesADatasetLongerThanWhatIsLeft() throws Exception {
        // Dataset 0x09 claims 12 bytes, only 4 follow.
        ISOMsg m = message(104, "09000C" + "DF170155");
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(m, "104.0x09"));
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(m, "104.0x09.0xDF17"));
        // ... also when the damaged dataset comes before the one asked for
        ISOMsg n = message(104, "080004" + "DF170155" + "0A0020" + "0102");
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(n, "104.0x0B"));
    }

    @Test
    void refusesBytesThatAreNotIdentifiers() throws Exception {
        ISOMsg zero = message(43, "050003" + "DF1800" + "000000");
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(zero, "43.0x06"));
        ISOMsg ff = message(43, "FF0001" + "00" + "050003" + "DF1800");
        assertThrows(IllegalArgumentException.class, () -> DatasetFinder.find(ff, "43.0x05"));
    }
}
