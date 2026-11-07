package com.example.practice;

import org.jpos.core.SimpleConfiguration;
import org.jpos.util.DirPoll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PayoutProcessorTest {
    @TempDir
    Path base;
    PayoutProcessor.Ledger ledger;

    @BeforeEach
    void setUp() {
        ledger = new PayoutProcessor.Ledger();         // the database: it survives every restart below
    }

    /** A node: a fresh DirPoll and processor on the same directories and the same ledger. */
    private DirPoll node() throws Exception {
        DirPoll dirPoll = new DirPoll();
        dirPoll.setPath(base.toString());
        dirPoll.setPollInterval(1);
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("archive", "true");
        cfg.put("archive.timestamp", "false");
        cfg.put("response.suffix", ".ack");
        dirPoll.setConfiguration(cfg);
        dirPoll.createDirs();
        dirPoll.setProcessor(new PayoutProcessor(ledger));
        return dirPoll;
    }

    /** Puts a file in request/ and lets DirPoll's own runner take it through run/ on this thread. */
    private void deliver(DirPoll node, String name, String content) throws Exception {
        Path f = Files.writeString(base.resolve("request").resolve(name), content, StandardCharsets.US_ASCII);
        node.new ProcessorRunner(f.toFile()).run();
    }

    private String answer(String name) throws Exception {
        return Files.readString(base.resolve("response").resolve(name), StandardCharsets.US_ASCII);
    }

    private boolean in(String dir, String name) {
        return Files.exists(base.resolve(dir).resolve(name));
    }

    private static final String RUN_0412 =
            "H|ACQ771|0412\nP|000000001234567|000000081000\nP|000000007654321|000000002250\nT|2|83250\n";

    @Test
    void differentRunsAreEachPaid() throws Exception {
        DirPoll node = node();
        deliver(node, "run-0412.pay", RUN_0412);
        deliver(node, "run-0413.pay", "H|ACQ771|0413\nP|000000001234567|000000000300\nT|1|300\n");
        assertEquals(3, ledger.payments().size());
        assertTrue(answer("run-0413.ack").endsWith(" merchants=1 total=300\n"));
    }

    @Test
    void aRunMovedBackAfterACrashIsAnsweredButNotPaidAgain() throws Exception {
        deliver(node(), "run-0412.pay", RUN_0412);
        Files.delete(base.resolve("response/run-0412.ack"));       // the crash came before the answer
        Files.delete(base.resolve("archive/run-0412.pay"));
        deliver(node(), "run-0412.pay", RUN_0412);                 // the runbook moved it back
        assertEquals(2, ledger.payments().size(), "each merchant is paid once");
        assertEquals("PAID ACQ771/0412 merchants=2 total=83250\n", answer("run-0412.ack"),
                "the sender still gets its answer");
        assertTrue(in("archive", "run-0412.pay"));
    }

    @Test
    void theSameRunUnderAnotherNameIsNotPaidAgain() throws Exception {
        DirPoll node = node();
        deliver(node, "run-0412.pay", RUN_0412);
        deliver(node, "resend-0412.pay", RUN_0412);                // the acquirer resent it by hand
        assertEquals(2, ledger.payments().size(), "each merchant is paid once");
        assertEquals("PAID ACQ771/0412 merchants=2 total=83250\n", answer("resend-0412.ack"));
    }

    @Test
    void anotherAcquirersRunWithTheSameNumberIsPaid() throws Exception {
        DirPoll node = node();
        deliver(node, "run-0412.pay", RUN_0412);
        deliver(node, "acq902-0412.pay", "H|ACQ902|0412\nP|000000005550001|000000004400\nT|1|4400\n");
        assertEquals(3, ledger.payments().size(), "ACQ902's run 0412 is not ACQ771's");
        assertEquals("PAID ACQ902/0412 merchants=1 total=4400\n", answer("acq902-0412.ack"));
    }

    @Test
    void aReusedRunNumberWithOtherPayoutsIsRefused() throws Exception {
        DirPoll node = node();
        deliver(node, "run-0412.pay", RUN_0412);
        deliver(node, "run-0412b.pay",
                "H|ACQ771|0412\nP|000000001234567|000000081000\nP|000000007654321|000000009999\nT|2|90999\n");
        assertTrue(in("bad", "run-0412b.pay"), "a person must decide which run 0412 is right");
        assertFalse(in("response", "run-0412b.ack"), "nobody is told it was paid");
        assertEquals(2, ledger.payments().size());
    }
}
