package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IssuerLimitTest {

    private static final AtomicInteger STAN = new AtomicInteger(400);

    private static ISOMsg purchase() throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000003150");
        m.set(11, String.format("%06d", STAN.incrementAndGet()));
        m.set(41, "29110001");
        return m;
    }

    private static ISOMsg approve(ISOMsg request) throws Exception {
        ISOMsg answer = (ISOMsg) request.clone();
        answer.setResponseMTI();
        answer.set(39, "00");
        return answer;
    }

    interface Step {
        void run() throws Exception;
    }

    /** Keeps n requests to the issuer in flight (each waits inside the issuer) while 'meanwhile' runs. */
    private static void whileWaiting(IssuerLimit limit, String issuerId, int n, Step meanwhile) throws Exception {
        if (n == 0) {
            meanwhile.run();
            return;
        }
        ISOMsg answer = limit.forward(purchase(), issuerId, request -> {
            whileWaiting(limit, issuerId, n - 1, meanwhile);
            return approve(request);
        });
        assertEquals("00", answer.getString(39), "a request that was let through gets the issuer's answer");
    }

    @Test
    void answersComeFromTheIssuerWithinItsLimit() throws Exception {
        IssuerLimit limit = new IssuerLimit(Map.of("issuer-k", 4, "issuer-m", 4));
        ISOMsg request = purchase();
        ISOMsg answer = limit.forward(request, "issuer-m", IssuerLimitTest::approve);
        assertEquals("0110", answer.getMTI());
        assertEquals("00", answer.getString(39));
        assertEquals(request.getString(11), answer.getString(11));

        List<String> codes = new ArrayList<>();
        whileWaiting(limit, "issuer-k", 3, () ->
                codes.add(limit.forward(purchase(), "issuer-k", IssuerLimitTest::approve).getString(39)));
        assertEquals(List.of("00"), codes, "the fourth request to an issuer with a limit of 4 is forwarded");
    }

    @Test
    void oneIssuerAtItsLimitLeavesTheOthersAlone() throws Exception {
        IssuerLimit limit = new IssuerLimit(Map.of("issuer-k", 4, "issuer-m", 4));
        List<String> codes = new ArrayList<>();
        whileWaiting(limit, "issuer-k", 4, () -> {
            codes.add(limit.forward(purchase(), "issuer-m", IssuerLimitTest::approve).getString(39));
            codes.add(limit.forward(purchase(), "issuer-m", IssuerLimitTest::approve).getString(39));
        });
        assertEquals(List.of("00", "00"), codes, "issuer-m has nothing in flight");
    }

    @Test
    void aFullIssuerIsAnsweredForAtOnce() throws Exception {
        IssuerLimit limit = new IssuerLimit(Map.of("issuer-k", 2, "issuer-m", 4));
        AtomicInteger asked = new AtomicInteger();
        List<ISOMsg> answers = new ArrayList<>();
        ISOMsg request = purchase();
        whileWaiting(limit, "issuer-k", 2, () ->
                answers.add(limit.forward(request, "issuer-k", r -> {
                    asked.incrementAndGet();
                    return approve(r);
                })));
        ISOMsg answer = answers.get(0);
        assertEquals(0, asked.get(), "the issuer is not asked");
        assertEquals("0110", answer.getMTI());
        assertEquals("91", answer.getString(39), "the issuer could not be reached; the card was not refused");
        assertEquals(request.getString(11), answer.getString(11));
        assertEquals(request.getString(41), answer.getString(41));
    }

    @Test
    void everyEndingGivesItsPlaceBack() throws Exception {
        IssuerLimit limit = new IssuerLimit(Map.of("issuer-k", 3, "issuer-m", 3));
        for (int i = 0; i < 3; i++) {
            assertThrows(IllegalStateException.class, () -> limit.forward(purchase(), "issuer-k", r -> {
                throw new IllegalStateException("link reset");
            }));
            assertEquals(null, limit.forward(purchase(), "issuer-k", r -> null), "no answer in time");
        }
        List<String> codes = new ArrayList<>();
        whileWaiting(limit, "issuer-k", 2, () ->
                codes.add(limit.forward(purchase(), "issuer-k", IssuerLimitTest::approve).getString(39)));
        assertEquals(List.of("00"), codes, "after failures and silences issuer-k still has all 3 places");
    }

    @Test
    void aRefusalTakesNoPlaceAndGivesNoneBack() throws Exception {
        IssuerLimit limit = new IssuerLimit(Map.of("issuer-k", 2, "issuer-m", 2));
        whileWaiting(limit, "issuer-k", 2, () -> {
            for (int i = 0; i < 3; i++)
                limit.forward(purchase(), "issuer-k", IssuerLimitTest::approve);
        });
        List<String> codes = new ArrayList<>();
        whileWaiting(limit, "issuer-k", 2, () ->
                codes.add(limit.forward(purchase(), "issuer-k", IssuerLimitTest::approve).getString(39)));
        assertEquals(List.of("91"), codes, "the limit is still 2 after the refusals");
    }
}
