package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoldingHostTest {

    /** A client connection: records what the host sends; a closed one refuses, as a socket would. */
    static class Client implements ISOSource {
        private final List<ISOMsg> sent = new ArrayList<>();
        private volatile boolean connected = true;

        @Override public synchronized void send(ISOMsg m) throws IOException {
            if (!connected) throw new IOException("connection closed");
            sent.add(m);
            notifyAll();
        }
        @Override public boolean isConnected() { return connected; }
        void close() { connected = false; }
        synchronized List<String> stans() {
            List<String> s = new ArrayList<>();
            for (ISOMsg m : sent) s.add(m.getString(11));
            return s;
        }
        synchronized boolean awaitCount(int n, long ms) throws InterruptedException {
            if (sent.size() < n) wait(ms);
            return sent.size() >= n;
        }
        synchronized ISOMsg first() { return sent.get(0); }
    }

    private final ExecutorService sessions = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "session");
        t.setDaemon(true);
        return t;
    });

    @AfterEach
    void stop() {
        sessions.shutdownNow();
    }

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(4, "000000003150");
        m.set(11, stan);
        m.set(41, "29110003");
        return m;
    }

    /** Calls process the way a session thread would; fails if process does not return. */
    private void process(HoldingHost host, Client client, String stan) throws Exception {
        ISOMsg request = purchase(stan);
        Future<Boolean> done = sessions.submit(() -> host.process(client, request));
        assertTrue(done.get(5, TimeUnit.SECONDS), "process returns, and reports the request handled");
    }

    @Test
    void aRequestThatIsNotHeldIsAnsweredAtOnce() throws Exception {
        HoldingHost host = new HoldingHost(Set.of("000931"));
        Client client = new Client();
        process(host, client, "000930");
        assertEquals(List.of("000930"), client.stans());
        assertEquals("0210", client.first().getMTI());
        assertEquals("00", client.first().getString(39));
        assertEquals(0, host.release(), "nothing was held");
    }

    @Test
    void aHeldAnswerArrivesAfterRelease() throws Exception {
        HoldingHost host = new HoldingHost(Set.of("000941"));
        Client client = new Client();
        ISOMsg request = purchase("000941");
        sessions.submit(() -> host.process(client, request));
        for (int i = 0; i < 250 && !client.awaitCount(1, 20); i++)
            host.release();
        assertEquals(List.of("000941"), client.stans());
        assertEquals("0210", client.first().getMTI());
        assertEquals("00", client.first().getString(39));
    }

    @Test
    void theSessionGoesOnWhileAnAnswerIsHeld() throws Exception {
        HoldingHost host = new HoldingHost(Set.of("000951"));
        Client client = new Client();
        process(host, client, "000951");
        assertEquals(List.of(), client.stans(), "held until release");
        process(host, client, "000952");
        assertEquals(List.of("000952"), client.stans(), "the next request on the connection is answered");
        assertEquals(1, host.release());
        assertEquals(List.of("000952", "000951"), client.stans());
    }

    @Test
    void releaseAnswersInTheOrderTheRequestsArrived() throws Exception {
        HoldingHost host = new HoldingHost(Set.of("000963", "000961", "000962"));
        Client client = new Client(), other = new Client();
        process(host, client, "000962");
        process(host, other, "000962");                 // another terminal, the same STAN
        process(host, client, "000963");
        process(host, client, "000961");
        assertEquals(4, host.release());
        assertEquals(List.of("000962", "000963", "000961"), client.stans());
        assertEquals(List.of("000962"), other.stans());
    }

    @Test
    void aClientThatLeftDoesNotStopTheOthers() throws Exception {
        HoldingHost host = new HoldingHost(Set.of("000971", "000972", "000973", "000974"));
        Client a = new Client(), b = new Client(), c = new Client();
        Client d = new Client() {                       // drops between the check and the send
            @Override public synchronized void send(ISOMsg m) throws IOException {
                throw new IOException("connection reset");
            }
        };
        process(host, a, "000971");
        process(host, b, "000972");
        process(host, d, "000974");
        process(host, c, "000973");
        b.close();
        assertEquals(2, host.release(), "two answers reached a client");
        assertEquals(List.of("000971"), a.stans());
        assertEquals(List.of("000973"), c.stans());
    }

    @Test
    void answersHeldBySeveralSessionsAtOnceAreAllReleased() throws Exception {
        HoldingHost host = new HoldingHost(Set.of("000991"));
        Client client = new Client();
        int sessionCount = 4, perSession = 3000;
        java.util.concurrent.CountDownLatch go = new java.util.concurrent.CountDownLatch(1);
        List<Future<?>> running = new ArrayList<>();
        for (int s = 0; s < sessionCount; s++)
            running.add(sessions.submit(() -> {
                go.await();
                for (int i = 0; i < perSession; i++)
                    host.process(client, purchase("000991"));
                return null;
            }));
        Future<Integer> releasing = sessions.submit(() -> {
            go.await();
            int n = 0;
            while (!running.stream().allMatch(Future::isDone))
                n += host.release();                     // the test releases while sessions hold
            return n;
        });
        go.countDown();
        for (Future<?> f : running) f.get(30, TimeUnit.SECONDS);
        int released = releasing.get(30, TimeUnit.SECONDS) + host.release();
        assertEquals(sessionCount * perSession, released, "every held answer is released once");
        assertEquals(sessionCount * perSession, client.stans().size());
    }

    @Test
    void eachHeldAnswerIsSentOnce() throws Exception {
        HoldingHost host = new HoldingHost(Set.of("000981"));
        Client client = new Client();
        process(host, client, "000981");
        assertEquals(1, host.release());
        assertEquals(0, host.release(), "already released");
        assertEquals(List.of("000981"), client.stans());
    }
}
