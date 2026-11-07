package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkMessagesTest {
    private static final Clock UTC_EVENING = Clock.fixed(Instant.parse("2027-03-12T23:40:05Z"), ZoneOffset.UTC);

    private static LinkMessages link(long lastStanUsed, Clock clock) throws Exception {
        return new LinkMessages("4410022", "KSK00051", lastStanUsed, clock);
    }

    @Test
    void eachKindCarriesWhatItNeeds() throws Exception {
        LinkMessages link = link(0, UTC_EVENING);
        ISOMsg echo = link.echo();
        ISOMsg text = link.merchantText("Card reader update tonight");
        assertEquals("0800", echo.getMTI());
        assertEquals("301", echo.getString(70));
        assertEquals("KSK00051", echo.getString(41));
        assertEquals("4410022", echo.getString(33));
        assertEquals("0620", text.getMTI());
        assertEquals("Card reader update tonight", text.getString(63));
        for (ISOMsg m : new ISOMsg[] { echo, text }) {
            assertTrue(m.getString(11).matches("\\d{6}"), "field 11 is six digits");
            assertTrue(m.getString(7).matches("\\d{10}"), "field 7 is MMddhhmmss");
            m.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
            m.pack();
        }
    }

    @Test
    void messagesOfOneKindFollowEachOther() throws Exception {
        LinkMessages link = link(41, UTC_EVENING);
        assertEquals("000042", link.echo().getString(11));
        assertEquals("000043", link.echo().getString(11));
    }

    @Test
    void eachMessageIsItsOwn() throws Exception {
        LinkMessages link = link(0, UTC_EVENING);
        ISOMsg first = link.signOffNotice();
        first.set(48, "MAINTENANCE 0200-0230");
        String firstStan = first.getString(11);
        ISOMsg second = link.signOffNotice();
        assertFalse(second.hasField(48), "the second notice carries the first one's text");
        assertEquals(firstStan, first.getString(11), "building the second notice changed the first");
    }

    @Test
    void everyKindTakesTheLinksNextNumber() throws Exception {
        LinkMessages link = link(700, UTC_EVENING);
        assertEquals("000701", link.echo().getString(11));
        assertEquals("000702", link.merchantText("Settlement at 23:00").getString(11));
        assertEquals("000703", link.signOffNotice().getString(11));
        assertEquals("000704", link.echo().getString(11));
    }

    @Test
    void afterTheLastNumberTheLinkStartsAgainAtOne() throws Exception {
        LinkMessages link = link(999998, UTC_EVENING);
        assertEquals("999999", link.echo().getString(11));
        assertEquals("000001", link.echo().getString(11));
    }

    @Test
    void theSendTimeIsGmtWhereverTheServerRuns() throws Exception {
        Clock tokyo = Clock.fixed(Instant.parse("2027-03-12T23:40:05Z"), ZoneId.of("Asia/Tokyo"));
        assertEquals("0312234005", link(0, tokyo).echo().getString(7));
    }
}
