package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.Dumpable;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogSource;
import org.junit.jupiter.api.Test;

class EvidenceKeeperTest {
    static final String REALM = "acq-channel/127.0.0.1:8000";

    /** What the store was handed. */
    static final class Store implements EvidenceKeeper.EvidenceStore {
        final List<String> parts = new ArrayList<>();
        final List<byte[]> bytes = new ArrayList<>();
        final List<String> realms = new ArrayList<>();

        @Override
        public void keep(String realm, String part, byte[] b) {
            realms.add(realm);
            parts.add(part);
            bytes.add(b);
        }
    }

    /** A declined 0110 for terminal 29110003, as the acquirer sends it. */
    static ISOMsg reply() throws ISOException {
        ISOMsg m = new ISOMsg("0110");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000003300");
        m.set(11, "000588");
        m.set(39, "51");
        m.set(41, "29110003");
        return m;
    }

    static byte[] wire() throws ISOException {
        ISOMsg m = reply();
        m.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        return m.pack();
    }

    /** A receive event shaped the way a channel shapes it after a failed unpack. */
    static LogEvent failedUnpack(Logger logger, byte[] header, byte[] data) {
        LogEvent ev = new LogEvent(new SimpleLogSource(logger, REALM), "receive");
        ev.addMessage(new ISOException("org.jpos.iso.IF_CHAR: Problem unpacking field 41"));
        if (header != null) {
            ev.addMessage("--- header ---");
            ev.addMessage(ISOUtil.hexdump(header));
        }
        ev.addMessage("--- data ---");
        ev.addMessage(ISOUtil.hexdump(data));
        return ev;
    }

    private static boolean anyRawDump(List<Object> payload) {
        return payload.stream().anyMatch(o -> o instanceof Dumpable
            || (o instanceof String && ((String) o).startsWith("0000  ")));
    }

    @Test
    void theBytesOfAFailedUnpackLeaveTheLogForTheStore() throws Exception {
        Store store = new Store();
        byte[] data = Arrays.copyOf(wire(), wire().length - 1);
        LogEvent ev = failedUnpack(new Logger(), null, data);
        new EvidenceKeeper(store).log(ev);
        assertFalse(anyRawDump(ev.getPayLoad()), "no raw bytes in the event any more");
        assertTrue(ev.getPayLoad().get(0) instanceof ISOException, "the exception is still logged");
        assertTrue(ev.getPayLoad().contains("--- data ---"), "the log still says bytes were there");
        assertEquals(List.of("data"), store.parts);
        assertEquals(List.of(REALM), store.realms);
    }

    @Test
    void anEventWithoutRawBytesIsLeftAsItIs() throws Exception {
        Store store = new Store();
        ISOMsg msg = reply();
        LogEvent ev = new LogEvent(new SimpleLogSource(new Logger(), REALM), "receive");
        ev.addMessage(msg);
        ev.addMessage("0000  this line is a note, not a dump");
        new EvidenceKeeper(store).log(ev);
        assertSame(msg, ev.getPayLoad().get(0));
        assertEquals("0000  this line is a note, not a dump", ev.getPayLoad().get(1));
        assertEquals(List.of(), store.parts);
    }

    @Test
    void theStoreGetsTheBytesTheChannelRead() throws Exception {
        Store store = new Store();
        byte[] data = Arrays.copyOf(wire(), wire().length - 1);
        new EvidenceKeeper(store).log(failedUnpack(new Logger(), null, data));
        assertArrayEquals(data, store.bytes.get(0));
    }

    @Test
    void aHeaderIsKeptAsWellAsTheData() throws Exception {
        Store store = new Store();
        byte[] tpdu = ISOUtil.hex2byte("6000120000");
        byte[] data = Arrays.copyOf(wire(), wire().length - 1);
        LogEvent ev = failedUnpack(new Logger(), tpdu, data);
        new EvidenceKeeper(store).log(ev);
        assertFalse(anyRawDump(ev.getPayLoad()));
        assertEquals(List.of("header", "data"), store.parts);
        assertArrayEquals(tpdu, store.bytes.get(0));
    }

    @Test
    void anImageAddedByDebugFilterIsWithheld() throws Exception {
        Store store = new Store();
        LogEvent ev = new LogEvent(new SimpleLogSource(new Logger(), REALM), "receive");
        ev.addMessage(reply());
        ev.addMessage(new Dumpable("image", wire()));
        new EvidenceKeeper(store).log(ev);
        assertFalse(anyRawDump(ev.getPayLoad()), "DebugFilter's image is raw bytes too");
        assertEquals(2, ev.getPayLoad().size());
    }

    @Test
    void theMessageInTheEventIsNeverChanged() throws Exception {
        Store store = new Store();
        ISOMsg msg = reply();
        LogEvent ev = new LogEvent(new SimpleLogSource(new Logger(), REALM), "receive");
        ev.addMessage(msg);
        ev.addMessage(new Dumpable("image", wire()));
        new EvidenceKeeper(store).log(ev);
        assertEquals("5555555555554444", msg.getString(2), "the message our code holds keeps its card");
    }

    @Test
    void theWritersAfterItStillGetTheEvent() throws Exception {
        Store store = new Store();
        Logger logger = new Logger();
        List<LogEvent> written = new ArrayList<>();
        logger.addListener(new EvidenceKeeper(store));
        logger.addListener(e -> { written.add(e); return e; });
        byte[] data = Arrays.copyOf(wire(), wire().length - 1);
        Logger.log(failedUnpack(logger, null, data));
        assertEquals(1, written.size(), "the writer after the keeper received the event");
        assertNotNull(written.get(0));
        assertTrue(written.get(0).getPayLoad().get(0) instanceof ISOException);
        assertEquals(1, store.parts.size());
    }
}
