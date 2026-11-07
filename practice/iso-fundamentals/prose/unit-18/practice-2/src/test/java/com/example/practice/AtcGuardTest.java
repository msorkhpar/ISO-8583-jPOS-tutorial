package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.practice.AtcGuard.Verdict;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

class AtcGuardTest {

    private static final String VISA = "4111111111111111";

    /** An authorization for card {@code pan} with sequence number {@code psn} (null: none) and counter {@code atcHex}. */
    private static ISOMsg request(String pan, String psn, String atcHex) throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(2, pan);
        m.set(4, "000000003310");
        if (psn != null)
            m.set(23, psn);
        m.set(55, ISOUtil.hex2byte("9F26080102030405060708" + "9F270180" + "9F3602" + atcHex + "9F0206000000003310"));
        return m;
    }

    @Test
    void risingCountersAreAcceptedAndAFailedCryptogramIsNot() throws Exception {
        AtcGuard guard = new AtcGuard();
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "000", "0031"), true));
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "000", "0032"), true));
        assertEquals(Verdict.ACCEPTED, guard.check(request("5555555555554444", "000", "0011"), true));
        assertEquals(Verdict.CRYPTOGRAM_FAILED, guard.check(request(VISA, "000", "0040"), false));
    }

    @Test
    void theSameCounterTwiceIsAReplay() throws Exception {
        AtcGuard guard = new AtcGuard();
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "000", "0057"), true));
        assertEquals(Verdict.REPLAYED, guard.check(request(VISA, "000", "0057"), true));
        assertEquals(Verdict.REPLAYED, guard.check(request(VISA, "000", "0056"), true));
    }

    @Test
    void aReplacementCardKeepsItsOwnCounter() throws Exception {
        AtcGuard guard = new AtcGuard();
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "000", "0150"), true));
        // the replacement card has the same number, sequence number 001, and starts counting again
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "001", "0003"), true));
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "001", "0004"), true));
        assertEquals(Verdict.REPLAYED, guard.check(request(VISA, "000", "0150"), true));
    }

    @Test
    void theCounterIsABinaryNumber() throws Exception {
        AtcGuard guard = new AtcGuard();
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "000", "0098"), true));
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "000", "00A3"), true));
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "000", "0100"), true));
        assertEquals(Verdict.REPLAYED, guard.check(request(VISA, "000", "00FF"), true));
    }

    @Test
    void aForgedCounterDoesNotLockOutTheCard() throws Exception {
        AtcGuard guard = new AtcGuard();
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "000", "0031"), true));
        assertEquals(Verdict.CRYPTOGRAM_FAILED, guard.check(request(VISA, "000", "FFFF"), false));
        assertEquals(Verdict.ACCEPTED, guard.check(request(VISA, "000", "0032"), true));
    }
}
