package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

class ChipConsistencyTest {

    private static final String CARD_PART = "9F26081122334455667788" + "9F270180" + "9F36020031";

    /** fields: number, value, number, value ...; de55 in hex, or null for none. */
    private static ISOMsg message(String de55, String... fields) throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(2, "5555555555554444");
        m.set(11, "004417");
        m.set(41, "29110001");
        for (int i = 0; i < fields.length; i += 2)
            m.set(Integer.parseInt(fields[i]), fields[i + 1]);
        if (de55 != null)
            m.set(55, ISOUtil.hex2byte(de55));
        return m;
    }

    @Test
    void agreeingChipDataAndMissingChipDataReportNothing() throws Exception {
        // An account verification: a zero amount, transaction type 00
        ISOMsg verification = message(CARD_PART + "9F0206000000000000" + "9C0100",
                "3", "000000", "4", "000000000000");
        assertEquals(List.of(), ChipConsistency.mismatches(verification));
        ISOMsg swiped = message(null, "3", "000000", "4", "000000004275");
        assertEquals(List.of(), ChipConsistency.mismatches(swiped));
    }

    @Test
    void everyDisagreementIsReportedInOrder() throws Exception {
        ISOMsg all = message(CARD_PART + "9A03261105" + "9C0100" + "5F2A020978" + "9F0206000000000000",
                "3", "200000", "4", "000000000700", "13", "0217", "49", "840");
        assertEquals(List.of("9F02", "5F2A", "9C", "9A"), ChipConsistency.mismatches(all));
        ISOMsg amountOnly = message(CARD_PART + "9F0206000000000000" + "9C0100",
                "3", "000000", "4", "000000000700");
        assertEquals(List.of("9F02"), ChipConsistency.mismatches(amountOnly));
    }

    @Test
    void numericChipValuesAreReadAsTheirDigits() throws Exception {
        ISOMsg refund = message(CARD_PART + "9F0206000000004275" + "9C0120", "3", "200000", "4", "000000004275");
        assertEquals(List.of(), ChipConsistency.mismatches(refund));
        ISOMsg cash = message(CARD_PART + "9F0206000000019990" + "9C0101", "3", "010000", "4", "000000019990");
        assertEquals(List.of(), ChipConsistency.mismatches(cash));
    }

    @Test
    void theChipCurrencyHasOneMoreDigitThanField49() throws Exception {
        assertEquals(List.of(), ChipConsistency.mismatches(message(CARD_PART + "5F2A020036", "49", "036")));
        assertEquals(List.of(), ChipConsistency.mismatches(message(CARD_PART + "5F2A020392", "49", "392")));
        assertEquals(List.of("5F2A"), ChipConsistency.mismatches(message(CARD_PART + "5F2A020392", "49", "036")));
    }

    @Test
    void theChipDateIsComparedByMonthAndDay() throws Exception {
        assertEquals(List.of(), ChipConsistency.mismatches(message(CARD_PART + "9A03270315", "13", "0315")));
        assertEquals(List.of("9A"), ChipConsistency.mismatches(message(CARD_PART + "9A03270315", "13", "2703")));
    }

    @Test
    void bytesInsideAnotherValueAreNotATag() throws Exception {
        // The cryptogram happens to contain 9C 01 20 and 9A 03 99 ...
        String de55 = "9F260800" + "9C0120" + "9A0399" + "00" + "9F270180" + "9C0100" + "9A03270315";
        assertEquals(List.of(), ChipConsistency.mismatches(message(de55, "3", "000000", "13", "0315")));
    }

    @Test
    void aValueWithoutItsFieldIsNotADisagreement() throws Exception {
        // 9F02 without DE 4, 9A without DE 13
        ISOMsg m = message(CARD_PART + "9F0206000000004275" + "9A03270315" + "9C0100", "3", "000000", "49", "036");
        assertEquals(List.of(), ChipConsistency.mismatches(m));
    }
}
