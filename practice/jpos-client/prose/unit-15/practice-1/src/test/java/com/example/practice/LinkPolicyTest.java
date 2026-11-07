package com.example.practice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkPolicyTest {
    private static final long FIRST = 3_000, LONGEST = 60_000;

    @Test
    void aRefusedSignOnIsNotRetriedAtOnce() {
        LinkPolicy p = new LinkPolicy(FIRST, LONGEST);
        long now = 7_200_000;
        assertTrue(p.maySignOn(now), "nothing has been refused yet");
        p.refused(now);
        assertFalse(p.maySignOn(now), "a refused sign-on must not be sent again at once");
        assertTrue(p.maySignOn(now + LONGEST), "after the longest wait the next attempt may go out");
    }

    @Test
    void anAcceptedSignOnLiftsTheWait() {
        LinkPolicy p = new LinkPolicy(FIRST, LONGEST);
        long now = 7_200_000;
        p.refused(now);
        p.signedOn();
        assertTrue(p.maySignOn(now), "after a success nothing stops the next sign-on");
    }

    @Test
    void theFirstRefusalWaitsTheFirstWait() {
        LinkPolicy p = new LinkPolicy(FIRST, LONGEST);
        assertEquals(7_203_000, p.refused(7_200_000));
        assertFalse(p.maySignOn(7_202_999));
        assertTrue(p.maySignOn(7_203_000));
    }

    @Test
    void eachRefusalInARowWaitsTwiceAsLongUpToTheLongest() {
        LinkPolicy p = new LinkPolicy(FIRST, LONGEST);
        long[] expected = { 3_000, 6_000, 12_000, 24_000, 48_000, 60_000, 60_000, 60_000 };
        long now = 7_200_000;
        for (int i = 0; i < expected.length; i++) {
            long next = p.refused(now);
            assertEquals(expected[i], next - now, "wait after refusal " + (i + 1));
            now = next;
        }
    }

    @Test
    void aNightLongOutageStillWaitsTheLongest() {
        LinkPolicy p = new LinkPolicy(FIRST, LONGEST);
        long now = 7_200_000;
        for (int i = 1; i <= 500; i++) {
            long next = p.refused(now);
            long wait = next - now;
            assertTrue(wait >= FIRST && wait <= LONGEST, "wait after refusal " + i + " was " + wait + " ms");
            if (i > 10)
                assertEquals(LONGEST, wait, "wait after refusal " + i);
            now = next;
        }
    }

    @Test
    void afterASuccessTheNextRefusalWaitsTheFirstWaitAgain() {
        LinkPolicy p = new LinkPolicy(FIRST, LONGEST);
        long now = 7_200_000;
        for (int i = 0; i < 4; i++)
            now = p.refused(now);
        p.signedOn();
        assertEquals(now + 3_000 + 900_000, p.refused(now + 900_000), "a new outage starts with the first wait");
    }
}
