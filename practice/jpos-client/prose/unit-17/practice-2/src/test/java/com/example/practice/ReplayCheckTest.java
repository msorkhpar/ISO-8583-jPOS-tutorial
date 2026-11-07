package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

class ReplayCheckTest {
    private final ReplayCheck check = new ReplayCheck(Set.of("7", "11", "37", "48.3"));

    /** An approved 0110 for terminal 29110003, as the simulator answers it. */
    private static ISOMsg response(String stan, String rrn) throws Exception {
        ISOMsg m = new ISOMsg("0110");
        m.set(3, "000000");
        m.set(4, "000000003300");
        m.set(7, "1107093000");
        m.set(11, stan);
        m.set(37, rrn);
        m.set(38, "A1B2C3");
        m.set(39, "00");
        m.set(41, "29110003");
        return m;
    }

    private static ISOMsg unpacked(ISOMsg m) throws Exception {
        GenericPackager p = new GenericPackager("jar:packager/iso87ascii.xml");
        m.setPackager(p);
        ISOMsg back = new ISOMsg();
        back.setPackager(p);
        back.unpack(m.pack());
        return back;
    }

    @Test
    void aReplayThatMatchesTheRecordingPasses() throws Exception {
        ISOMsg recorded = response("000201", "731100000201");
        ISOMsg replayed = response("000588", "731200000588");
        replayed.set(7, "1108101500");
        assertEquals(List.of(), check.differences(recorded, replayed));
        assertEquals(List.of(), check.differences(unpacked(recorded), unpacked(replayed)));
    }

    @Test
    void aChangedResponseFailsOnTheFieldsThatChanged() throws Exception {
        ISOMsg recorded = response("000201", "731100000201");
        ISOMsg replayed = response("000588", "731200000588");
        replayed.set(39, "51");
        replayed.unset(38);
        replayed.set(44, "5");
        assertEquals(List.of("38", "39", "44"), check.differences(recorded, replayed));
    }

    @Test
    void binaryValuesAreComparedByWhatTheyHold() throws Exception {
        ISOMsg recorded = response("000201", "731100000201");
        ISOMsg replayed = response("000588", "731200000588");
        recorded.set(55, ISOUtil.hex2byte("910A1122334455667788303072"));
        replayed.set(55, ISOUtil.hex2byte("910A1122334455667788303072"));
        assertEquals(List.of(), check.differences(recorded, replayed), "the same chip data in two arrays");
        replayed.set(55, ISOUtil.hex2byte("910A1122334455667788303172"));
        assertEquals(List.of("55"), check.differences(recorded, replayed));
    }

    @Test
    void subFieldsAreComparedOneByOne() throws Exception {
        ISOMsg recorded = response("000201", "731100000201");
        ISOMsg replayed = response("000588", "731200000588");
        recorded.set("48.1", "LOYALTY");
        replayed.set("48.1", "LOYALTY");
        recorded.set("48.3", "SESSION-17");
        replayed.set("48.3", "SESSION-42");
        assertEquals(List.of(), check.differences(recorded, replayed), "48.3 changes every run");
        recorded.set("48.2", "120");
        replayed.set("48.2", "95");
        assertEquals(List.of("48.2"), check.differences(recorded, replayed));
        replayed.unset(48);
        assertEquals(List.of("48.1", "48.2"), check.differences(recorded, replayed));
    }

    @Test
    void aFieldMissingFromOneUnpackedResponseIsReportedOnce() throws Exception {
        ISOMsg recorded = response("000201", "731100000201");
        ISOMsg replayed = response("000588", "731200000588");
        replayed.unset(38);
        assertEquals(List.of("38"), check.differences(unpacked(recorded), unpacked(replayed)));
    }

    @Test
    void differencesComeInFieldOrder() throws Exception {
        ISOMsg recorded = response("000201", "731100000201");
        ISOMsg replayed = response("000588", "731200000588");
        replayed.set(4, "000000003000");
        replayed.set(14, "2712");
        replayed.set(39, "05");
        assertEquals(List.of("4", "14", "39"), check.differences(recorded, replayed));
    }
}
