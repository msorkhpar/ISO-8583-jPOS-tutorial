package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class PaymentsEndpointTest {

    /** A host that approves, or stays silent for the requests it is told to drop. */
    static final class ScriptedHost implements PaymentsEndpoint.Host {
        final List<ISOMsg> received = new ArrayList<>();
        int silent;                       // how many of the next requests get no answer
        CountDownLatch firstArrived;      // when set, counted down as the first request arrives...
        CountDownLatch releaseFirst;      // ...which is then answered only once this opens

        @Override
        public ISOMsg request(ISOMsg m) throws ISOException {
            boolean first;
            boolean drop;
            synchronized (this) {
                received.add((ISOMsg) m.clone());
                first = received.size() == 1;
                drop = silent > 0;
                if (drop)
                    silent--;
            }
            if (first && firstArrived != null) {
                firstArrived.countDown();
                try {
                    if (!releaseFirst.await(5, TimeUnit.SECONDS))
                        throw new IllegalStateException("the test never released the first request");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            if (drop)
                return null;
            ISOMsg r = (ISOMsg) m.clone();
            r.setResponseMTI();
            r.set(39, "00");
            r.set(38, "B" + m.getString(11).substring(1));
            return r;
        }

        synchronized int purchases() {
            int n = 0;
            for (ISOMsg m : received)
                if ("0200".equals(m.getString(0)))
                    n++;
            return n;
        }
    }

    private static PaymentsEndpoint.Payment payment(String amount) {
        return new PaymentsEndpoint.Payment("4111111111111111", new BigDecimal(amount), "29110001");
    }

    @Test
    void aFirstCallIsSentAndApproved() throws Exception {
        ScriptedHost host = new ScriptedHost();
        PaymentsEndpoint endpoint = new PaymentsEndpoint(host, new PaymentsEndpoint.KeyTable());

        PaymentsEndpoint.Result r = endpoint.pay("shop7-0001", payment("12.40"));

        assertEquals("APPROVED", r.status());
        assertEquals(1, host.purchases());
        assertEquals("000000001240", host.received.get(0).getString(4));
        assertEquals("B" + host.received.get(0).getString(11).substring(1), r.approvalCode());
    }

    @Test
    void aRetryAfterALostAnswerGetsTheFirstAnswer() throws Exception {
        ScriptedHost host = new ScriptedHost();
        PaymentsEndpoint endpoint = new PaymentsEndpoint(host, new PaymentsEndpoint.KeyTable());

        PaymentsEndpoint.Result first = endpoint.pay("shop7-0002", payment("12.40"));
        PaymentsEndpoint.Result retry = endpoint.pay("shop7-0002", payment("12.40"));

        assertEquals(first, retry);
        assertEquals(1, host.purchases());
    }

    @Test
    void aRetryAfterNoAnswerIsToldItFailedAndNothingIsSentAgain() throws Exception {
        ScriptedHost host = new ScriptedHost();
        host.silent = 1;
        PaymentsEndpoint endpoint = new PaymentsEndpoint(host, new PaymentsEndpoint.KeyTable());

        PaymentsEndpoint.Result first = endpoint.pay("shop7-0003", payment("88.00"));
        PaymentsEndpoint.Result retry = endpoint.pay("shop7-0003", payment("88.00"));

        assertEquals("FAILED", first.status());
        assertEquals("FAILED", retry.status());
        assertEquals(1, host.purchases());
        assertEquals(1, endpoint.reversals().size());
        assertEquals("0420", endpoint.reversals().get(0).getMTI());
    }

    @Test
    void aKeyReusedForAnotherPaymentIsRefused() throws Exception {
        ScriptedHost host = new ScriptedHost();
        PaymentsEndpoint endpoint = new PaymentsEndpoint(host, new PaymentsEndpoint.KeyTable());

        endpoint.pay("shop7-0004", payment("12.40"));
        PaymentsEndpoint.Result other = endpoint.pay("shop7-0004", payment("19.00"));

        assertEquals("REFUSED", other.status());
        assertEquals(null, other.approvalCode());
        assertEquals(1, host.purchases());
    }

    @Test
    void aRetryWhileTheFirstCallWaitsIsNotSent() throws Exception {
        ScriptedHost host = new ScriptedHost();
        host.firstArrived = new CountDownLatch(1);
        host.releaseFirst = new CountDownLatch(1);
        PaymentsEndpoint endpoint = new PaymentsEndpoint(host, new PaymentsEndpoint.KeyTable());
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<PaymentsEndpoint.Result> first = pool.submit(() -> endpoint.pay("shop7-0005", payment("31.15")));
            assertTrue(host.firstArrived.await(5, TimeUnit.SECONDS), "the first call never reached the host");

            PaymentsEndpoint.Result retry = endpoint.pay("shop7-0005", payment("31.15"));
            host.releaseFirst.countDown();

            assertEquals("IN_PROGRESS", retry.status());
            assertEquals("APPROVED", first.get(5, TimeUnit.SECONDS).status());
            assertEquals(1, host.purchases());
        } finally {
            host.releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void twoPaymentsAlikeWithTheirOwnKeysAreBothSent() throws Exception {
        ScriptedHost host = new ScriptedHost();
        PaymentsEndpoint endpoint = new PaymentsEndpoint(host, new PaymentsEndpoint.KeyTable());

        PaymentsEndpoint.Result one = endpoint.pay("shop7-0006", payment("3.80"));
        PaymentsEndpoint.Result two = endpoint.pay("shop7-0007", payment("3.80"));

        assertEquals("APPROVED", one.status());
        assertEquals("APPROVED", two.status());
        assertNotEquals(one.approvalCode(), two.approvalCode());
        assertEquals(2, host.purchases());
    }
}
