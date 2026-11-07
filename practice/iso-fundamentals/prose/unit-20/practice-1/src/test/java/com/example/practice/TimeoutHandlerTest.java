package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import com.example.practice.TimeoutHandler.Crash;
import com.example.practice.TimeoutHandler.Db;
import com.example.practice.TimeoutHandler.Outcome;
import com.example.practice.TimeoutHandler.Status;

class TimeoutHandlerTest {

    /** The terminal hears something; hearing it is a step the process can die after. */
    static final class Told implements TimeoutHandler.Terminal {
        final Db db;
        final List<String> heard = new ArrayList<>();
        Told(Db db) { this.db = db; }
        @Override
        public void tell(String stan, Outcome outcome) {
            heard.add(stan + " " + outcome);
            db.step();
        }
    }

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000001845");
        m.set(7, "0314081502");
        m.set(11, stan);
        m.set(32, "4400");
        m.set(35, "5555555555554444=29081010000000000000");
        m.set(41, "29110001");
        m.set(49, "978");
        m.set(52, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        return m;
    }

    private static ISOMsg answer(ISOMsg request, String rc) throws Exception {
        ISOMsg r = (ISOMsg) request.clone();
        r.setResponseMTI();
        r.set(39, rc);
        return r;
    }

    private static boolean queued(Db db, String originalStan) {
        return db.queue().stream().anyMatch(m -> m.getString(90).substring(4, 10).equals(originalStan));
    }

    // ---- main cases ----

    @Test
    void aTimeoutEndsWithTheReversalQueuedAndTheTerminalTold() throws Exception {
        Db db = new Db();
        Told terminal = new Told(db);
        TimeoutHandler h = new TimeoutHandler(db, terminal);

        h.onNoAnswer(purchase("000057"), "000058", "0314081532");

        assertEquals(1, db.queue().size());
        ISOMsg rev = db.queue().get(0);
        assertEquals("0420", rev.getMTI());
        assertEquals("000058", rev.getString(11));
        assertTrue(queued(db, "000057"), "field 90 names the purchase");
        assertEquals(Status.REVERSING, db.journal("000057"));
        assertEquals(List.of("000057 FAILED"), terminal.heard);
    }

    @Test
    void aLateDeclineChangesNothing() throws Exception {
        Db db = new Db();
        Told terminal = new Told(db);
        TimeoutHandler h = new TimeoutHandler(db, terminal);
        ISOMsg p = purchase("000091");
        h.onNoAnswer(p, "000092", "0314090010");

        h.onLateAnswer(answer(p, "51"));

        assertTrue(queued(db, "000091"));
        assertEquals(Status.REVERSING, db.journal("000091"));
        assertEquals(List.of("000091 FAILED"), terminal.heard);
    }

    // ---- edge cases ----

    @Test
    void theTerminalHearsOfTheFailureOnlyOnceTheReversalIsStored() throws Exception {
        for (int dieAfter = 1; dieAfter <= 6; dieAfter++) {
            Db db = new Db();
            Told terminal = new Told(db);
            db.dieAfterSteps(dieAfter);
            try {
                new TimeoutHandler(db, terminal).onNoAnswer(purchase("000204"), "000205", "0314101500");
            } catch (Crash died) {
                // what survived is what the database holds and what the terminal heard
            }
            if (terminal.heard.contains("000204 FAILED"))
                assertTrue(queued(db, "000204"), "dying after step " + dieAfter
                        + ": the terminal was told, and no reversal is stored");
        }
    }

    @Test
    void theJournalAndTheQueueAgreeWhereverTheProcessDies() throws Exception {
        for (int dieAfter = 1; dieAfter <= 6; dieAfter++) {
            Db db = new Db();
            db.dieAfterSteps(dieAfter);
            try {
                new TimeoutHandler(db, new Told(db)).onNoAnswer(purchase("000310"), "000311", "0314111500");
            } catch (Crash died) {
                // as above
            }
            assertEquals(queued(db, "000310"), db.journal("000310") == Status.REVERSING,
                    "dying after step " + dieAfter + ": queue " + db.queue().size()
                            + ", journal " + db.journal("000310"));
        }
    }

    @Test
    void aLateApprovalLeavesTheReversalQueued() throws Exception {
        Db db = new Db();
        Told terminal = new Told(db);
        TimeoutHandler h = new TimeoutHandler(db, terminal);
        ISOMsg p = purchase("000420");
        h.onNoAnswer(p, "000421", "0314121500");

        h.onLateAnswer(answer(p, "00"));

        assertTrue(queued(db, "000420"), "the reversal is still queued");
        assertEquals(List.of("000420 FAILED"), terminal.heard);
    }

    @Test
    void aLateApprovalIsRecorded() throws Exception {
        Db db = new Db();
        TimeoutHandler h = new TimeoutHandler(db, new Told(db));
        ISOMsg p = purchase("000533");
        h.onNoAnswer(p, "000534", "0314131500");

        h.onLateAnswer(answer(p, "00"));

        assertEquals(Status.LATE_APPROVAL, db.journal("000533"));
    }
}
