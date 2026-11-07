package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class CodeTranslatorTest {

    /** The gateway's table: written code by code from the processor's and the terminals' lists. */
    private static final Map<String, String> TABLE = Map.of(
            "0000", "00", "0001", "08", "0003", "11",
            "1001", "54", "1017", "55", "2009", "43",
            "4000", "00", "9001", "00", "9109", "96");

    private final CodeTranslator gateway = new CodeTranslator(TABLE);

    private void told(String actionCode, String expected) {
        assertEquals(expected, gateway.toTerminal(actionCode), "toTerminal(" + actionCode + ")");
        assertEquals(expected, gateway.answerFor(actionCode), "answerFor(" + actionCode + ")");
        assertFalse(gateway.mustReverse(actionCode), "mustReverse(" + actionCode + ")");
    }

    private void reversed(String actionCode) {
        assertTrue(gateway.mustReverse(actionCode), "mustReverse(" + actionCode + ")");
        assertEquals("96", gateway.answerFor(actionCode), "answerFor(" + actionCode + ")");
    }

    @Test
    void theTableAnswersForTheCodesItLists() {
        told("0000", "00");
        told("0001", "08");
        told("0003", "11");
        told("1001", "54");
        told("1017", "55");
        told("2009", "43");
        told("9109", "96");
    }

    @Test
    void acknowledgementsListedInTheTableAreAnswered00() {
        told("4000", "00");
        told("9001", "00");
    }

    @Test
    void anApprovalOffTheTableIsReversedNotPassedOn() {
        reversed("0006");
        reversed("0005");
    }

    @Test
    void anErrorOffTheTableIsNotADecline() {
        told("9112", "96");
        told("9111", "96");
    }

    @Test
    void aDenialOffTheTableIsStillADenial() {
        told("1035", "05");
        told("1033", "05");
    }

    @Test
    void anAnswerWithoutAUsableCodeDecidesNothing() {
        reversed(null);
        reversed("");
        reversed("05");
    }
}
