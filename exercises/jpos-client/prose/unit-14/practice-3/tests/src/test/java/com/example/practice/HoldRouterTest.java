package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.space.LocalSpace;
import org.jpos.space.TSpace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoldRouterTest {
    private static final long TTL = 7L * 24 * 3600 * 1000;      // holds last a week

    private static ISOMsg preauth(String terminal, String stan, String time) throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(3, "000000");
        m.set(4, "000000030000");
        m.set(7, time);
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    /** A completion for `p`, with its own STAN and time, pointing at `p` through field 90. */
    private static ISOMsg completion(ISOMsg p, String stan, String time) throws Exception {
        ISOMsg m = new ISOMsg("0220");
        m.set(3, "000000");
        m.set(4, "000000027450");
        m.set(7, time);
        m.set(11, stan);
        m.set(41, p.getString(41));
        m.set(90, "0100" + p.getString(11) + p.getString(7) + "0000000000000000000000");
        return m;
    }

    @Test
    void preauthsTakeTurnsAndCompletionsAreAnswered() throws Exception {
        Rig rig = new Rig();
        HoldRouter router = new HoldRouter(new TSpace<>(), TTL, rig.links);
        ISOMsg p1 = preauth("52000014", "000310", "0702184500");
        ISOMsg p2 = preauth("52000014", "000311", "0702184630");
        assertEquals("0110", router.request(p1, 2000).getMTI());
        assertEquals("0110", router.request(p2, 2000).getMTI());
        assertEquals("000310", rig.hosts[0].received().get(0).getString(11));
        assertEquals("000311", rig.hosts[1].received().get(0).getString(11));
        ISOMsg a1 = router.request(completion(p1, "000340", "0704101500"), 2000);
        ISOMsg a2 = router.request(completion(p2, "000341", "0704101620"), 2000);
        assertNotNull(a1);
        assertNotNull(a2);
        assertEquals("0230", a1.getMTI());
    }

    @Test
    void aCompletionGoesToTheDataCentreThatHoldsIt() throws Exception {
        Rig rig = new Rig();
        HoldRouter router = new HoldRouter(new TSpace<>(), TTL, rig.links);
        ISOMsg p1 = preauth("52000014", "000310", "0702184500");
        router.request(p1, 2000);                                          // data centre 1
        router.request(preauth("52000027", "001902", "0702184511"), 2000); // data centre 2
        router.request(preauth("52000033", "000045", "0702184519"), 2000); // data centre 1
        router.request(completion(p1, "000340", "0704101500"), 2000);
        assertTrue(rig.hosts[0].received().stream().anyMatch(m -> m.getString(11).equals("000340")),
                "the completion reached data centre 1");
    }

    @Test
    void aCompletionWaitsWhileItsDataCentreIsDown() throws Exception {
        Rig rig = new Rig();
        HoldRouter router = new HoldRouter(new TSpace<>(), TTL, rig.links);
        ISOMsg p1 = preauth("52000014", "000310", "0702184500");
        router.request(p1, 2000);                                          // data centre 1
        rig.down(0);
        ISOMsg c = completion(p1, "000340", "0704101500");
        assertNull(router.request(c, 2000), "nothing can settle the hold now");
        assertEquals(1, rig.hosts[0].received().size() + rig.hosts[1].received().size(), "the completion was not sent");
        rig.up(0);
        assertNotNull(router.request(c, 2000));
        assertEquals("000340", rig.hosts[0].received().get(1).getString(11));
    }

    @Test
    void aCompletionIsMatchedThroughFieldNinety() throws Exception {
        Rig rig = new Rig();
        HoldRouter router = new HoldRouter(new TSpace<>(), TTL, rig.links);
        ISOMsg p1 = preauth("52000014", "000500", "0703090000");
        router.request(p1, 2000);                                          // data centre 1
        router.request(preauth("52000014", "000501", "0703090112"), 2000); // data centre 2
        // the completion's own STAN and time happen to be those of the second pre-authorization
        router.request(completion(p1, "000501", "0703090112"), 2000);
        assertEquals(2, rig.hosts[0].received().size(), "the completion reached data centre 1");
    }

    @Test
    void twoTerminalsWithTheSameStanAndTimeKeepTheirOwnHolds() throws Exception {
        Rig rig = new Rig();
        HoldRouter router = new HoldRouter(new TSpace<>(), TTL, rig.links);
        ISOMsg p1 = preauth("52000014", "000077", "0705120000");
        router.request(p1, 2000);                                          // data centre 1
        router.request(preauth("52000089", "000077", "0705120000"), 2000); // data centre 2
        router.request(completion(p1, "000078", "0706081500"), 2000);
        assertEquals(2, rig.hosts[0].received().size(), "terminal 52000014's completion reached data centre 1");
    }

    @Test
    void aHoldIsRememberedOnlyAsLongAsItLasts() throws Exception {
        Rig rig = new Rig();
        RecordingSpace holds = new RecordingSpace();
        HoldRouter router = new HoldRouter(holds, TTL, rig.links);
        router.request(preauth("52000014", "000310", "0702184500"), 2000);
        assertTrue(holds.writes > 0, "the route was remembered");
        assertEquals(holds.writes, holds.writesWithTtl, "every remembered route expires with the hold");
    }

    @Test
    void aCompletionWithNoKnownHoldStillGoesOut() throws Exception {
        Rig rig = new Rig();
        HoldRouter router = new HoldRouter(new TSpace<>(), TTL, rig.links);
        ISOMsg old = preauth("52000014", "000208", "0628200000");          // sent before a restart
        ISOMsg answer = router.request(completion(old, "000340", "0704101500"), 2000);
        assertNotNull(answer, "an unknown hold can still be settled; the acquirer decides");
        assertEquals(1, rig.hosts[0].received().size() + rig.hosts[1].received().size());
    }

    // ---- test support ----

    /** A TSpace that counts the writes made with and without a lifetime. */
    static final class RecordingSpace extends TSpace<String, Object> {
        int writes, writesWithTtl;
        @Override public void put(String k, Object v) { writes++; super.put(k, v); }
        @Override public void put(String k, Object v, long ttl) { writes++; if (ttl == TTL) writesWithTtl++; super.put(k, v, ttl); }
        @Override public void out(String k, Object v) { writes++; super.out(k, v); }
        @Override public void out(String k, Object v, long ttl) { writes++; if (ttl == TTL) writesWithTtl++; super.out(k, v, ttl); }
    }

    private static final java.util.concurrent.atomic.AtomicInteger SPACES = new java.util.concurrent.atomic.AtomicInteger();

    /** Two real QMUXes, one per data centre, with a host behind each that answers 00. */
    static final class Rig {
        final LocalSpace<String, Object> sp;
        final MUX[] links = new MUX[2];
        final Host[] hosts = new Host[2];

        @SuppressWarnings("unchecked")
        Rig() throws Exception {
            int n = SPACES.incrementAndGet();
            String space = "tspace:holds-" + n;
            sp = (LocalSpace<String, Object>) org.jpos.space.SpaceFactory.getSpace(space);
            for (int i = 0; i < 2; i++) {
                org.jdom2.Element e = new org.jdom2.Element("mux");
                e.addContent(new org.jdom2.Element("space").setText(space));
                e.addContent(new org.jdom2.Element("in").setText("dc" + (i + 1) + "-receive"));
                e.addContent(new org.jdom2.Element("out").setText("dc" + (i + 1) + "-send"));
                e.addContent(new org.jdom2.Element("ready").setText("dc" + (i + 1) + "-channel.ready"));
                org.jpos.q2.iso.QMUX q = new org.jpos.q2.iso.QMUX();
                q.setName("dc" + (i + 1) + "-mux-" + n);
                q.setPersist(e);
                q.setConfiguration(new org.jpos.core.SimpleConfiguration());
                q.init();
                q.start();
                links[i] = q;
                hosts[i] = new Host(sp, "dc" + (i + 1) + "-send", "dc" + (i + 1) + "-receive");
                up(i);
            }
        }

        void up(int i) { sp.put("dc" + (i + 1) + "-channel.ready", new java.util.Date()); }
        void down(int i) { sp.inp("dc" + (i + 1) + "-channel.ready"); }
    }

    static final class Host implements org.jpos.space.SpaceListener<String, Object> {
        private final LocalSpace<String, Object> sp;
        private final String in;
        private final java.util.List<ISOMsg> received = new java.util.ArrayList<>();

        Host(LocalSpace<String, Object> sp, String out, String in) {
            this.sp = sp;
            this.in = in;
            sp.addListener(out, this);
        }

        synchronized java.util.List<ISOMsg> received() {
            return new java.util.ArrayList<>(received);
        }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            synchronized (this) { received.add(request); }
            try {
                ISOMsg response = (ISOMsg) request.clone();
                response.setResponseMTI();
                response.set(39, "00");
                sp.out(in, response);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
