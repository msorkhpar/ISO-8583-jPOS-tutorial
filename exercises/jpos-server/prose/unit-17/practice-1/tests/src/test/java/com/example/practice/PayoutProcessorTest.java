package com.example.practice;

import org.jpos.core.SimpleConfiguration;
import org.jpos.util.DirPoll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayoutProcessorTest {
    @TempDir
    Path base;
    PayoutProcessor.Ledger ledger;
    PayoutProcessor processor;
    DirPoll dirPoll;

    @BeforeEach
    void setUp() throws Exception {
        ledger = new PayoutProcessor.Ledger();
        processor = new PayoutProcessor(ledger);
        dirPoll = new DirPoll();
        dirPoll.setPath(base.toString());
        dirPoll.setPollInterval(1);                       // a retry waits ten intervals: 10 ms
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("archive", "true");
        cfg.put("response.suffix", ".ack");
        dirPoll.setConfiguration(cfg);
        dirPoll.createDirs();
        dirPoll.setProcessor(processor);
    }

    /** Puts a file in request/ and lets DirPoll's own runner take it through run/ on this thread. */
    private void deliver(String name, String content) throws Exception {
        Path f = Files.writeString(base.resolve("request").resolve(name), content, StandardCharsets.US_ASCII);
        dirPoll.new ProcessorRunner(f.toFile()).run();
    }

    private boolean in(String dir, String name) {
        return Files.exists(base.resolve(dir).resolve(name));
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private DirPoll.DirPollException thrownBy(String name, String content) {
        return assertThrows(DirPoll.DirPollException.class, () -> processor.process(name, bytes(content)));
    }

    @Test
    void aCompleteRunIsPaidAnsweredAndArchived() throws Exception {
        deliver("run-0317.pay", "H|ACQ771|0317\r\n"
                + "P|000000001234567|000000250075\r\n"
                + "\r\n"
                + "P|000000007654321|000000000930\r\n"
                + "P|000000001234567|000000010000\r\n"
                + "T|3|261005\r\n");
        assertTrue(in("archive", "run-0317.pay"), "the run is archived");
        assertEquals("PAID ACQ771/0317 merchants=3 total=261005\n",
                Files.readString(base.resolve("response/run-0317.ack"), StandardCharsets.US_ASCII));
        assertEquals(List.of("ACQ771/0317 000000001234567 250075",
                             "ACQ771/0317 000000007654321 930",
                             "ACQ771/0317 000000001234567 10000"), ledger.payments());
    }

    @Test
    void aRunWithoutItsTrailerIsTakenAgainLater() {
        DirPoll.DirPollException e = thrownBy("run-0318.pay",
                "H|ACQ771|0318\nP|000000001234567|000000000500\nP|000000007654321|000000000700\n");
        assertTrue(e.isRetry(), "the sender may still be writing: DirPoll should come back to it");
        assertTrue(ledger.payments().isEmpty(), "nobody is paid from half a run");
    }

    @Test
    void anEmptyFileWaitsForItsContent() throws Exception {
        deliver("run-0319.pay", "");
        assertTrue(in("request", "run-0319.pay"), "an empty file is back in request/, waiting");
        assertFalse(in("bad", "run-0319.pay"));
        assertTrue(ledger.payments().isEmpty());
    }

    @Test
    void aMalformedRecordSendsTheRunToAPerson() {
        DirPoll.DirPollException e = thrownBy("run-0320.pay",
                "H|ACQ771|0320\nP|000000001234567|000000000500\nP|000000007654321|12.50\nT|2|1750\n");
        assertFalse(e.isRetry(), "the record will not get better by waiting");
        assertTrue(ledger.payments().isEmpty());
    }

    @Test
    void aCountThatDisagreesIsRefused() {
        DirPoll.DirPollException e = thrownBy("run-0321.pay",
                "H|ACQ771|0321\nP|000000001234567|000000000500\nP|000000007654321|000000000700\nT|3|1200\n");
        assertFalse(e.isRetry());
        assertTrue(ledger.payments().isEmpty(), "a merchant may be missing: nobody is paid");
    }

    @Test
    void aTotalThatDisagreesIsRefused() {
        DirPoll.DirPollException e = thrownBy("run-0322.pay",
                "H|ACQ771|0322\nP|000000001234567|000000000500\nP|000000007654321|000000000700\nT|2|1300\n");
        assertFalse(e.isRetry());
        assertTrue(ledger.payments().isEmpty(), "an amount may be wrong: nobody is paid");
    }

    @Test
    void eachRunIsCheckedOnItsOwn() throws Exception {
        deliver("run-0323.pay", "H|ACQ771|0323\nP|000000001234567|000000000400\nT|1|400\n");
        deliver("run-0324.pay", "H|ACQ771|0324\nP|000000007654321|000000000900\nT|1|900\n");
        assertTrue(in("archive", "run-0323.pay"));
        assertTrue(in("archive", "run-0324.pay"), "the second run is judged on its own payouts");
        assertEquals(2, ledger.payments().size());
    }
}
