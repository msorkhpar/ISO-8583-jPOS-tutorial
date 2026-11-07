package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.example.practice.CardNumberFinder.Finding;
import org.junit.jupiter.api.Test;

class CardNumberFinderTest {

    @Test
    void findsCardNumbersTypedInGroupsOfFour() {
        String ticket = "Charged twice on 5555 5555 5555 4444, and once on 4111-1111-1111-1111.";
        List<Finding> found = CardNumberFinder.find(ticket);
        assertEquals(2, found.size(), "two card numbers in " + found);
        assertEquals(ticket.indexOf("5555 "), found.get(0).offset());
        assertEquals(ticket.indexOf("4111-"), found.get(1).offset());
        assertTrue(found.get(0).shown().startsWith("555555") && found.get(0).shown().endsWith("4444"),
                "the report shows which card it was: " + found.get(0).shown());
    }

    @Test
    void aTicketWithoutCardNumbersHasNoFindings() {
        assertEquals(List.of(), CardNumberFinder.find("Order 88213 shipped on 2026-11-05, call +44 20 7946 0018."));
        assertEquals(List.of(), CardNumberFinder.find(""));
    }

    @Test
    void aNumberThatFailsTheCheckDigitIsNotACard() {
        assertEquals(List.of(), CardNumberFinder.find("Your case reference is 4111 1111 1111 1112, keep it."));
    }

    @Test
    void aCardGroupedAsItIsPrintedOnAFifteenDigitCardIsFound() {
        String ticket = "The 3782 822463 10005 card was declined at the hotel.";
        List<Finding> found = CardNumberFinder.find(ticket);
        assertEquals(1, found.size(), "one card number in " + found);
        assertEquals(ticket.indexOf("3782"), found.get(0).offset());
    }

    @Test
    void aCardGluedToLettersIsFound() {
        String ticket = "terminal log: %B4000000000000000006^TEST/CARD^2812101";
        List<Finding> found = CardNumberFinder.find(ticket);
        assertEquals(1, found.size(), "one card number in " + found);
        assertEquals(ticket.indexOf("4000"), found.get(0).offset());
    }

    @Test
    void digitsInsideALongerNumberAreNotACard() {
        assertEquals(List.of(), CardNumberFinder.find("Batch 40000000000000000060000 settled at 23:00."));
    }

    @Test
    void theReportNeverHoldsTheNumberInClear() {
        List<Finding> found = CardNumberFinder.find("Please refund 6011 0000 0000 0000 001 by Friday.");
        assertEquals(1, found.size(), "one card number in " + found);
        assertEquals("601100_________0001", found.get(0).shown());
        assertFalse(found.get(0).shown().contains("0000000"), "no run of the clear number is left");
    }
}
