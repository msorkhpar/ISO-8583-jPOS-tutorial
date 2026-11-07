package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCardGuardTest {

    /** The ranges this network issued for the certification. */
    private static TestCardGuard guard() {
        return new TestCardGuard(List.of("4012888888", "5105105105"));
    }

    private static ISOMsg keyed(String pan, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, "000000003175");
        m.set(11, stan);
        m.set(41, "29110001");
        return m;
    }

    private static ISOMsg swiped(String track2, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, "000000001980");
        m.set(11, stan);
        m.set(35, track2);
        m.set(41, "29110001");
        return m;
    }

    @Test
    void issuedTestCardsAndCardlessMessagesGoThrough() throws Exception {
        TestCardGuard g = guard();
        ISOMsg visa = keyed("4012888888881881", "000211");
        assertSame(visa, g.check(visa), "an issued test card is sent as it is");
        ISOMsg mc = keyed("5105105105105100", "000212");
        assertSame(mc, g.check(mc));
        ISOMsg echo = new ISOMsg("0800");
        echo.set(11, "000213");
        echo.set(70, "301");
        assertSame(echo, g.check(echo), "a message without a card has nothing to refuse");
    }

    @Test
    void aMalformedNumberFromNoIssuedRangeIsRefused() throws Exception {
        assertThrows(TestCardGuard.Refused.class, () -> guard().check(keyed("6011111111111118", "000221")));
    }

    @Test
    void aWellFormedCardFromNoIssuedRangeIsRefused() throws Exception {
        assertThrows(TestCardGuard.Refused.class, () -> guard().check(keyed("6011111111111117", "000231")),
                "a correct check digit does not make a number a test card");
        assertThrows(TestCardGuard.Refused.class, () -> guard().check(keyed("378282246310005", "000232")));
    }

    @Test
    void aMistypedTestCardIsRefused() throws Exception {
        assertThrows(TestCardGuard.Refused.class, () -> guard().check(keyed("4012888888881882", "000241")),
                "a mistyped number from an issued range gets 14 from the host, not the case's answer");
        assertThrows(TestCardGuard.Refused.class, () -> guard().check(keyed("5105105105105109", "000242")));
    }

    @Test
    void aSwipedPurchaseIsCheckedThroughItsTrack() throws Exception {
        TestCardGuard g = guard();
        assertThrows(TestCardGuard.Refused.class,
                () -> g.check(swiped("6011111111111117=29121010000000000000", "000251")),
                "a swiped purchase carries its card number in the track");
        assertThrows(TestCardGuard.Refused.class,
                () -> g.check(swiped("6011111111111117D29121010000000000000", "000252")));
        ISOMsg ok = swiped("5105105105105100=29121010000000000000", "000253");
        assertSame(ok, g.check(ok), "an issued test card on a track goes through");
    }

    @Test
    void theRefusalNamesNoCardInFull() throws Exception {
        String pan = new String("6011111111111117");
        TestCardGuard.Refused r = assertThrows(TestCardGuard.Refused.class, () -> guard().check(keyed(pan, "000261")));
        String text = String.valueOf(r.getMessage());
        assertFalse(text.contains(pan), "the build log must not get the full card number: " + text);
        assertTrue(text.contains("1117"), "the tester still needs to tell which card it was: " + text);
    }
}
