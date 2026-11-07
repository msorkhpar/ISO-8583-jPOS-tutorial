package com.example.practice;

import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.jpos.util.ProtectedLogListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Drives CardExpiry directly, with a logger whose chain is the production one: the filter, then a writer. */
class CardExpiryTest {
    private static final String REQUEST = ContextConstants.REQUEST.toString();
    private static final String RESPONSE = ContextConstants.RESPONSE.toString();
    private static final String PAN = "5555555555554444";
    private static final String TRACK2 = PAN + "=26091010000000000000";
    private static final byte[] PIN_BLOCK = ISOUtil.hex2byte("6B21C3D08E7FA914");

    private final List<String> written = new ArrayList<>();
    private final List<String> realms = new ArrayList<>();
    private final List<String> entries = new ArrayList<>();
    private CardExpiry participant;

    @BeforeEach
    void setUp() throws Exception {
        ProtectedLogListener protect = new ProtectedLogListener();
        SimpleConfiguration p = new SimpleConfiguration();
        p.put("protect", "2 35 45");
        p.put("wipe", "52 55");
        protect.setConfiguration(p);
        Logger logger = new Logger();
        logger.setName("practice-" + System.nanoTime());
        logger.addListener(protect);
        logger.addListener((LogEvent ev) -> {
            written.add(ev.toString());
            realms.add(ev.getRealm());
            for (Object o : ev.getPayLoad())
                entries.add(String.valueOf(o));
            return ev;
        });
        participant = new CardExpiry();
        participant.setLogger(logger, "card-expiry");      // what logger="Q2" realm="card-expiry" does under Q2
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("this-month", "2026-11");
        participant.setConfiguration(cfg);
    }

    private static ISOMsg purchase(String stan, String expiry) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, PAN);
        m.set(3, "000000");
        m.set(4, "000000006480");
        m.set(11, stan);
        if (expiry != null)
            m.set(14, expiry);
        m.set(35, TRACK2);
        m.set(41, "29110042");
        m.set(52, PIN_BLOCK);
        return m;
    }

    private static Context contextFor(ISOMsg request) {
        Context ctx = new Context();
        ctx.put(REQUEST, request);
        return ctx;
    }

    @Test
    void aCurrentCardGoesThrough() throws Exception {
        Context ctx = contextFor(purchase("000512", "2705"));
        int vote = participant.prepare(7310L, ctx);
        assertEquals(TransactionConstants.PREPARED, vote & TransactionConstants.PREPARED, "a valid card is not refused");
        assertNull(ctx.get(RESPONSE), "an approval is built later in the chain");
    }

    @Test
    void anExpiredCardIsRefusedAndExplained() throws Exception {
        Context ctx = contextFor(purchase("000512", "2609"));
        assertEquals(TransactionConstants.ABORTED, participant.prepare(7310L, ctx));
        ISOMsg response = ctx.get(RESPONSE);
        assertEquals("54", response.getString(39));
        assertEquals("0210", response.getMTI());
        assertEquals(1, written.size(), "one event for the refused purchase: " + written);
        assertEquals("card-expiry", realms.get(0), "the event is the participant's own, in its realm");
        assertTrue(written.get(0).contains("000512"), "the event shows which purchase it is: " + written.get(0));
        assertTrue(written.get(0).contains("2609"), "the event says which expiry was read: " + written.get(0));
    }

    @Test
    void anUnreadableExpiryIsAnsweredAndExplained() throws Exception {
        Context ctx = contextFor(purchase("000613", "2613"));
        assertEquals(TransactionConstants.ABORTED, participant.prepare(7402L, ctx));
        assertEquals("30", ((ISOMsg) ctx.get(RESPONSE)).getString(39), "the terminal still gets an answer");
        assertEquals(1, written.size(), "an unreadable expiry leaves one event too: " + written);
        assertTrue(written.get(0).contains("2613"), "the event shows what could not be read: " + written.get(0));
    }

    @Test
    void anApprovalIsExplainedToo() throws Exception {
        participant.prepare(7520L, contextFor(purchase("000714", "2811")));
        assertEquals(1, written.size(), "a purchase that went through leaves one event: " + written);
        assertTrue(written.get(0).contains("000714"), written.get(0));
    }

    @Test
    void noCardDataReachesTheWriterInClear() throws Exception {
        participant.prepare(7310L, contextFor(purchase("000512", "2609")));
        String all = String.join("\n", written);
        assertFalse(all.contains(PAN), "clear card number in the log:\n" + all);
        assertFalse(all.contains("26091010000000000000"), "clear track 2 in the log:\n" + all);
        assertFalse(all.contains("6B21C3D08E7FA914"), "clear PIN block in the log:\n" + all);
        assertTrue(all.contains("555555______4444"), "the masked card number shows whose purchase it was:\n" + all);
    }

    @Test
    void eachTransactionIsExplainedOnItsOwn() throws Exception {
        participant.prepare(7310L, contextFor(purchase("000512", "2609")));
        participant.prepare(7311L, contextFor(purchase("000513", "2510")));
        assertEquals(2, written.size(), "one event per transaction: " + written);
        assertTrue(written.get(1).contains("000513"), written.get(1));
        assertFalse(written.get(1).contains("000512"), "the second event holds the first transaction's purchase:\n" + written.get(1));
    }

    @Test
    void theMessagesKeepTheirCardNumber() throws Exception {
        ISOMsg request = purchase("000512", "2609");
        Context ctx = contextFor(request);
        participant.prepare(7310L, ctx);
        assertEquals(PAN, request.getString(2), "the request still has to be processed with its card number");
        assertEquals(PAN, ((ISOMsg) ctx.get(RESPONSE)).getString(2), "the answer is built from the request as it arrived");
    }

    @Test
    void theEventLeadsToTheTransactionManagersEvent() throws Exception {
        participant.prepare(7310L, contextFor(purchase("000512", "2609")));
        assertEquals(1, written.size(), written.toString());
        assertTrue(entries.stream().anyMatch(e -> e.contains("7310")),
            "nothing in the event matches the TransactionManager's event for transaction 7310:\n" + written.get(0));
    }
}
