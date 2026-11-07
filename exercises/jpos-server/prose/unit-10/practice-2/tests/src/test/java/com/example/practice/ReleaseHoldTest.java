package com.example.practice;

import com.example.practice.ReleaseHold.HoldTable;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.q2.Q2;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Runs ReleaseHold in a real TransactionManager deployed by Q2 in this JVM. */
class ReleaseHoldTest {
    private static final HoldTable HOLDS = new HoldTable();

    @Test
    void aReversalIsAnsweredAsAReplyToItself() throws Exception {
        HOLDS.open("ACC-1001", 80000);
        HOLDS.place("HTL00007", "004410", "1105221500", "ACC-1001", 25000);
        ISOMsg r = send(reversal("0400", "HTL00007", "004418", "1106091200", "004410", "1105221500"));
        assertEquals("0410", r.getMTI());
        assertEquals("004418", r.getString(11), "the answer carries the reversal's own STAN");
        assertEquals("00", r.getString(39));
    }

    @Test
    void theHoldIsLifted() throws Exception {
        HOLDS.open("ACC-2002", 50000);
        HOLDS.place("PMP00031", "000077", "1106070512", "ACC-2002", 15000);
        send(reversal("0400", "PMP00031", "000081", "1106070755", "000077", "1106070512"));
        assertEquals(50000, HOLDS.available("ACC-2002"), "the cardholder gets the held amount back");
    }

    @Test
    void aRepeatLiftsNothingMore() throws Exception {
        HOLDS.open("ACC-3003", 30000);
        HOLDS.place("HTL00009", "120005", "1106130000", "ACC-3003", 12000);
        ISOMsg rev = reversal("0400", "HTL00009", "120009", "1106131010", "120005", "1106130000");
        send(rev);
        ISOMsg repeat = reversal("0401", "HTL00009", "120009", "1106131010", "120005", "1106130000");
        assertEquals("00", send(repeat).getString(39), "the repeat is answered like the first");
        send(repeat);
        assertEquals(30000, HOLDS.available("ACC-3003"), "the hold comes back once, however often it is asked");
    }

    @Test
    void aReversalOfNothingIsAnswered00() throws Exception {
        HOLDS.open("ACC-4004", 9000);
        ISOMsg r = send(reversal("0400", "PMP00032", "000310", "1106201400", "000302", "1106201350"));
        assertEquals("0410", r.getMTI());
        assertEquals("00", r.getString(39), "a pre-authorization that never placed a hold has nothing left to undo");
        assertEquals(9000, HOLDS.available("ACC-4004"));
    }

    // ---- requests ----

    private static ISOMsg reversal(String mti, String terminal, String stan, String transmitted,
                                   String originalStan, String originalTransmitted) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "4111111111111111");
        m.set(3, "000000");
        m.set(4, "000000010000");
        m.set(7, transmitted);
        m.set(11, stan);
        m.set(41, terminal);
        m.set(49, "978");
        m.set(90, "0100" + originalStan + originalTransmitted + "00000000000" + "00000000000");
        return m;
    }

    private static ISOMsg send(ISOMsg m) throws Exception {
        BlockingQueue<ISOMsg> sent = new LinkedBlockingQueue<>();
        BlockingQueue<Boolean> ended = new LinkedBlockingQueue<>();
        Context ctx = new Context();
        ctx.put("ENDED", ended);
        ctx.put(ContextConstants.REQUEST.toString(), m);
        ctx.put(ContextConstants.SOURCE.toString(), new ISOSource() {
            @Override public void send(ISOMsg r) { sent.add(r); }
            @Override public boolean isConnected() { return true; }
        });
        SpaceFactory.getSpace().out("practice.reversals", ctx);
        assertNotNull(ended.poll(20, TimeUnit.SECONDS), "the transaction did not end");
        ISOMsg r = sent.poll(5, TimeUnit.SECONDS);
        assertNotNull(r, "the acquirer got no answer");
        return r;
    }

    // ---- test support: Q2 in this JVM ----

    private static Q2 q2;

    @BeforeAll
    static void startQ2() throws Exception {
        NameRegistrar.register("holds", HOLDS);
        Path deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-reversals' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='practice.reversals'/>\n"
          + "  <property name='sessions' value='1'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.ReleaseHold'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <participant class='com.example.practice.ReleaseHoldTest$Finished'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-reversals", 15000), "the TransactionManager did not start");
    }

    @AfterAll
    static void stopQ2() {
        if (q2 != null) q2.shutdown(true);
    }

    /** Last on both paths: tells the test the transaction ended. */
    public static class Finished implements AbortParticipant {
        @Override public int prepare(long id, Serializable context) { return PREPARED | READONLY; }
        @Override public int prepareForAbort(long id, Serializable context) { return PREPARED | READONLY; }
        @Override public void commit(long id, Serializable context) { ended(context).add(Boolean.TRUE); }
        @Override public void abort(long id, Serializable context) { ended(context).add(Boolean.FALSE); }

        @SuppressWarnings("unchecked")
        private static BlockingQueue<Boolean> ended(Serializable context) {
            return (BlockingQueue<Boolean>) ((Context) context).get("ENDED");
        }
    }
}
