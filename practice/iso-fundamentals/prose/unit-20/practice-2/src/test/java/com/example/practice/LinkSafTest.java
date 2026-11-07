package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class LinkSafTest {
    private static final long S = 1000, MIN = 60 * S, H = 60 * MIN;

    private static ISOMsg advice(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(4, "000000004720");
        m.set(11, stan);
        m.set(41, "29110001");
        return m;
    }

    /** base 20 s, cap 15 min, 6 attempts, 2 hours from the first attempt */
    private static LinkSaf network() {
        return new LinkSaf(20 * S, 15 * MIN, 6, 2 * H);
    }

    /** Fails the head at the moment it is due, and returns that moment. */
    private static long failWhenDue(LinkSaf saf) throws Exception {
        long t = saf.nextDueAt();
        assertNotNull(saf.due(t), "the head is due at " + t);
        saf.failed(t);
        return t;
    }

    // ---- main cases ----

    @Test
    void advicesGoOutInTheOrderStoredAndLeaveWhenAnswered() throws Exception {
        LinkSaf saf = network();
        saf.store(advice("0220", "000871"), 5 * S);
        saf.store(advice("0120", "000872"), 6 * S);

        ISOMsg first = saf.due(7 * S);
        assertEquals("000871", first.getString(11));
        saf.acknowledged();
        ISOMsg second = saf.due(7 * S);
        assertEquals("000872", second.getString(11));
        assertEquals("0120", second.getMTI());
        saf.acknowledged();
        assertNull(saf.due(8 * S));
        assertEquals(0, saf.size());
    }

    @Test
    void anUnansweredAdviceWaitsThenGoesOutAsARepeat() throws Exception {
        LinkSaf saf = network();
        saf.store(advice("0420", "000903"), 0);
        ISOMsg sent = saf.due(1 * S);
        assertEquals("0420", sent.getMTI());
        sent.set(4, "000000000001");               // the sender's copy, changed after sending
        saf.failed(1 * S);

        assertNull(saf.due(2 * S), "not due right after a failure");
        ISOMsg again = saf.due(1 * S + 15 * MIN);
        assertNotNull(again);
        assertEquals("0421", again.getMTI());
        assertEquals("000903", again.getString(11));
        assertEquals("000000004720", again.getString(4));
        assertEquals(1, saf.size());
    }

    // ---- edge cases ----

    @Test
    void theFirstWaitIsTheBaseAndEachFailureDoublesIt() throws Exception {
        LinkSaf saf = network();
        saf.store(advice("0220", "001114"), 0);
        long[] expected = { 20 * S, 40 * S, 80 * S, 160 * S };
        for (long wait : expected) {
            long t = failWhenDue(saf);
            assertEquals(t + wait, saf.nextDueAt(), "wait after a failure at " + t);
        }
    }

    @Test
    void theWaitStopsGrowingAtTheCap() throws Exception {
        LinkSaf saf = new LinkSaf(45 * S, 4 * MIN, 9, 24 * H);
        saf.store(advice("0220", "001250"), 0);
        long[] expected = { 45 * S, 90 * S, 180 * S, 240 * S, 240 * S, 240 * S };
        for (long wait : expected) {
            long t = failWhenDue(saf);
            assertEquals(t + wait, saf.nextDueAt(), "wait after a failure at " + t);
        }
    }

    @Test
    void theWaitStaysAtTheCapAfterManyFailures() throws Exception {
        LinkSaf saf = new LinkSaf(20 * S, 15 * MIN, 500, 365 * 24 * H);
        saf.store(advice("0420", "001377"), 0);
        for (int failures = 1; failures <= 90; failures++) {
            long t = failWhenDue(saf);
            if (failures >= 7)
                assertEquals(t + 15 * MIN, saf.nextDueAt(), "wait after failure " + failures);
        }
    }

    @Test
    void theWindowCountsFromTheFirstAttempt() throws Exception {
        LinkSaf saf = new LinkSaf(20 * S, 15 * MIN, 100, 2 * H);
        saf.store(advice("0420", "001402"), 0);
        long first = 3 * H;                       // the link was down for three hours after storing
        saf.linkUp(first);
        assertNotNull(saf.due(first));
        saf.failed(first);
        assertEquals(1, saf.size(), "parked although its first attempt was just now");

        long last = first;
        while (saf.size() == 1) {
            last = failWhenDue(saf);
            assertTrue(last - first < 2 * H, "tried " + (last - first) / MIN + " min after the first attempt");
        }
        assertTrue(last + 15 * MIN - first >= 2 * H,
                "parked after the attempt at " + (last - first) / MIN + " min, with time left for another");
        assertEquals(1, saf.parked().size());
    }

    @Test
    void aReconnectMakesTheHeadDueAtOnceWithoutAFreshStart() throws Exception {
        LinkSaf saf = network();
        saf.store(advice("0420", "001588"), 0);
        long t = failWhenDue(saf);
        t = failWhenDue(saf);
        t = failWhenDue(saf);

        long up = t + 10 * S;
        assertNull(saf.due(up), "still waiting before the reconnect");
        saf.linkUp(up);
        ISOMsg m = saf.due(up);
        assertNotNull(m, "due at once after the reconnect");
        assertEquals("0421", m.getMTI());

        saf.failed(up);                            // the fourth failure
        assertEquals(up + 160 * S, saf.nextDueAt());
        failWhenDue(saf);
        failWhenDue(saf);                          // the sixth
        assertEquals(0, saf.size(), "six attempts in all, the reconnect gave none back");
        assertEquals(1, saf.parked().size());
    }

    @Test
    void laterAdvicesWaitWhileTheHeadWaits() throws Exception {
        LinkSaf saf = network();
        saf.store(advice("0420", "001630"), 0);
        saf.store(advice("0220", "001631"), 0);
        saf.due(0);
        saf.failed(0);

        assertNull(saf.due(5 * S), "nothing goes out while the head waits");
        assertEquals("001630", saf.due(20 * S).getString(11));
    }

    @Test
    void anAdviceAtItsLimitIsParkedAsStoredAndTheNextGoesOut() throws Exception {
        LinkSaf saf = network();
        saf.store(advice("0420", "001745"), 0);
        saf.store(advice("0220", "001746"), 0);
        long t = 0;
        for (int i = 0; i < 6; i++)
            t = failWhenDue(saf);

        assertEquals(1, saf.parked().size(), "parked, not dropped");
        assertEquals("0420", saf.parked().get(0).getMTI());
        assertEquals("001745", saf.parked().get(0).getString(11));
        assertEquals("001746", saf.due(t).getString(11));
    }
}
