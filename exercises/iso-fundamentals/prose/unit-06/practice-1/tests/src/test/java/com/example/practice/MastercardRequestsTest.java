package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.practice.MastercardRequests.ProcessingCode;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class MastercardRequestsTest {

    private static ISOMsg responseWith(String code) throws Exception {
        ISOMsg response = new ISOMsg();
        response.setMTI("0210");
        response.set(39, code);
        return response;
    }

    @Test
    void buildsRequestsAndReadsTheirType() throws Exception {
        ISOMsg advance = MastercardRequests.request(ProcessingCode.CASH_ADVANCE, "5413330089020011", "50000", "MERCHANT01");
        assertEquals("0200", advance.getMTI());
        assertEquals("5413330089020011", advance.getString(2));
        assertEquals("010000", advance.getString(3));
        assertEquals("50000", advance.getString(4));
        assertEquals("MERCHANT01", advance.getString(42));
        assertFalse(advance.hasField(37));
        assertEquals(ProcessingCode.CASH_ADVANCE, MastercardRequests.typeOf(advance));

        ISOMsg refund = MastercardRequests.refund("5413330089020011", "25000", "MERCHANT01", "123456789012");
        assertEquals("0200", refund.getMTI());
        assertEquals("200000", refund.getString(3));
        assertEquals("25000", refund.getString(4));
        assertEquals("123456789012", refund.getString(37));
        assertEquals(ProcessingCode.REFUND, MastercardRequests.typeOf(refund));

        assertEquals(ProcessingCode.VOID, ProcessingCode.fromCode("020000"));
        assertEquals(ProcessingCode.PURCHASE, ProcessingCode.fromCode("000000"));

        assertTrue(MastercardRequests.isApproved(responseWith("00")));
        assertFalse(MastercardRequests.isApproved(responseWith("05")));
    }

    @Test
    void refusesAnUnknownProcessingCode() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> ProcessingCode.fromCode("990000"));
        assertThrows(IllegalArgumentException.class, () -> ProcessingCode.fromCode("20"));
        ISOMsg odd = new ISOMsg();
        odd.setMTI("0200");
        odd.set(3, "310000");
        assertThrows(IllegalArgumentException.class, () -> MastercardRequests.typeOf(odd));

        assertEquals(ProcessingCode.REFUND, ProcessingCode.fromCode("201000"));
        assertEquals(ProcessingCode.PURCHASE, ProcessingCode.fromCode("000030"));
    }

    @Test
    void aResponseWithoutField39IsNotApproved() throws Exception {
        ISOMsg silent = new ISOMsg();
        silent.setMTI("0210");
        assertFalse(MastercardRequests.isApproved(silent));
    }
}
