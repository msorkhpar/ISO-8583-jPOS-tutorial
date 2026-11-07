package com.example.practice;

import com.example.practice.LinkPolicy.EchoVerdict;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkPolicyTest {
    private static LinkPolicy policy(int echoLimit) {
        LinkPolicy p = new LinkPolicy(2_000, 40_000, echoLimit);
        p.signedOn();
        return p;
    }

    @Test
    void oneLostEchoKeepsTheLink() {
        LinkPolicy p = policy(3);
        assertEquals(EchoVerdict.KEEP, p.echoFailed());
    }

    @Test
    void aHostThatStopsAnsweringIsEventuallyLeft() {
        LinkPolicy p = policy(3);
        List<EchoVerdict> verdicts = new ArrayList<>();
        for (int i = 0; i < 5; i++)
            verdicts.add(p.echoFailed());
        assertEquals(EchoVerdict.KEEP, verdicts.get(0));
        assertTrue(verdicts.contains(EchoVerdict.SIGN_ON_AGAIN), "five failures in a row: " + verdicts);
    }

    @Test
    void failuresSpreadOverTheWeekNeverReachTheLimit() {
        LinkPolicy p = policy(3);
        for (int day = 1; day <= 10; day++) {
            assertEquals(EchoVerdict.KEEP, p.echoFailed(), "failure on day " + day);
            p.echoAnswered();
        }
    }

    @Test
    void theFailureThatReachesTheLimitAsksForANewSignOn() {
        LinkPolicy three = policy(3);
        assertEquals(EchoVerdict.KEEP, three.echoFailed());
        assertEquals(EchoVerdict.KEEP, three.echoFailed());
        assertEquals(EchoVerdict.SIGN_ON_AGAIN, three.echoFailed(), "third failure in a row with a limit of 3");
        LinkPolicy one = policy(1);
        assertEquals(EchoVerdict.SIGN_ON_AGAIN, one.echoFailed(), "a limit of 1 reacts to the first failure");
    }

    @Test
    void aFreshSignOnIsJudgedOnItsOwnEchoes() {
        LinkPolicy p = policy(3);
        p.echoFailed();
        p.echoFailed();
        p.signedOn();
        assertEquals(EchoVerdict.KEEP, p.echoFailed(), "first failure after the new sign-on");
        assertEquals(EchoVerdict.KEEP, p.echoFailed(), "second failure after the new sign-on");
        assertEquals(EchoVerdict.SIGN_ON_AGAIN, p.echoFailed(), "third failure after the new sign-on");
    }
}
