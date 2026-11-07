package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmRequestsTest {
    private static final String VISA = "4111111111111111";
    private static final String MC = "5555555555554444";

    /** 20:45:10 GMT on 14 February is 02:15:10 on 15 February in Kolkata. */
    private static final Clock KOLKATA = Clock.fixed(Instant.parse("2027-02-14T20:45:10Z"), ZoneId.of("Asia/Kolkata"));

    private static AtmRequests atm(Clock clock, long lastUsed) {
        AtomicLong counter = new AtomicLong(lastUsed);
        return new AtmRequests("ATM00417", clock, counter::incrementAndGet);
    }

    private static int stan(ISOMsg m) {
        return Integer.parseInt(m.getString(11));
    }

    @Test
    void eachRequestCarriesItsOwnFieldsAndPacks() throws Exception {
        AtmRequests atm = atm(KOLKATA, 316);
        ISOMsg inquiry = atm.balanceInquiry(VISA);
        ISOMsg cash = atm.withdrawal(MC, 6000, "356");
        ISOMsg signOn = atm.signOn();

        assertEquals("0100", inquiry.getMTI());
        assertEquals("310000", inquiry.getString(3));
        assertEquals("0200", cash.getMTI());
        assertEquals("010000", cash.getString(3));
        assertEquals("000000006000", cash.getString(4));
        assertEquals("356", cash.getString(49));
        assertEquals("ATM00417", inquiry.getString(41));
        assertEquals("ATM00417", cash.getString(41));
        assertEquals("0800", signOn.getMTI());
        assertEquals("001", signOn.getString(70));
        for (ISOMsg m : new ISOMsg[] { inquiry, cash }) {
            assertTrue(m.getString(7).matches("\\d{10}"), "field 7 is MMddhhmmss");
        }
        assertEquals(stan(inquiry) + 1, stan(cash), "the link counts its messages");

        GenericPackager packager = new GenericPackager("jar:packager/iso87ascii.xml");
        for (ISOMsg m : new ISOMsg[] { inquiry, cash, signOn }) {
            m.setPackager(packager);
            m.pack();
        }
    }

    @Test
    void fieldSevenReadsTheSameWhereverTheAtmStands() throws Exception {
        java.util.TimeZone jvmZone = java.util.TimeZone.getDefault();
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Pacific/Auckland"));   // the server's zone
        try {
            assertEquals("0214204510", atm(KOLKATA, 0).withdrawal(VISA, 2000, "356").getString(7));
            Clock chicago = Clock.fixed(Instant.parse("2027-02-14T20:45:10Z"), ZoneId.of("America/Chicago"));
            assertEquals("0214204510", atm(chicago, 0).balanceInquiry(MC).getString(7));
        } finally {
            java.util.TimeZone.setDefault(jvmZone);
        }
    }

    @Test
    void theCounterCarriesOnPastSixNines() throws Exception {
        AtmRequests atm = atm(KOLKATA, 999_998);
        assertEquals("999999", atm.balanceInquiry(VISA).getString(11));
        assertEquals("000001", atm.balanceInquiry(VISA).getString(11));
        assertEquals("000002", atm.withdrawal(VISA, 2000, "356").getString(11));
    }

    @Test
    void theStanInMemoryIsTheStanOnTheWire() throws Exception {
        AtmRequests atm = atm(KOLKATA, 57);
        assertEquals("000058", atm.withdrawal(MC, 10000, "356").getString(11),
                "the host echoes all six digits, and this value is what gets stored and compared");
    }

    @Test
    void theSignOnIsNumberedLikeEveryOtherMessage() throws Exception {
        AtmRequests atm = atm(KOLKATA, 4400);
        ISOMsg before = atm.balanceInquiry(VISA);
        ISOMsg signOn = atm.signOn();
        ISOMsg after = atm.balanceInquiry(VISA);
        assertTrue(signOn.hasField(11), "the host matches the sign-on's answer by its trace number");
        assertEquals(stan(before) + 1, stan(signOn));
        assertEquals(stan(signOn) + 1, stan(after));
    }
}
