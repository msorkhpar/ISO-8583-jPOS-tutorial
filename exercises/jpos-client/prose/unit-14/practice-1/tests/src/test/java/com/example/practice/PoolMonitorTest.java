package com.example.practice;

import org.jpos.q2.iso.MUXPool;
import org.jpos.q2.iso.QMUX;
import org.jpos.space.LocalSpace;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolMonitorTest {

    @Test
    void signedOnLinksAreInUseAndADroppedOneIsNamed() throws Exception {
        Rig rig = new Rig(true);
        rig.connect(0, new Date(1_700_000_000_000L));
        rig.connect(1, new Date(1_700_000_005_000L));
        rig.signOn(0);
        rig.signOn(1);
        PoolMonitor monitor = new PoolMonitor(rig.sp, true, rig.muxes);
        assertEquals(List.of(), monitor.skipped());
        assertTrue(rig.pool.isConnected());

        rig.disconnect(1);
        assertEquals(List.of("acq-mux-2"), monitor.skipped());
        rig.disconnect(0);
        assertEquals(List.of("acq-mux-1", "acq-mux-2"), monitor.skipped());
        assertFalse(rig.pool.isConnected(), "the pool agrees: nothing is usable");
    }

    @Test
    void aConnectedLinkThatHasNotSignedOnIsNamed() throws Exception {
        Rig rig = new Rig(true);
        rig.connect(0, new Date(1_700_000_000_000L));
        rig.connect(1, new Date(1_700_000_005_000L));
        rig.signOn(0);
        PoolMonitor monitor = new PoolMonitor(rig.sp, true, rig.muxes);
        assertEquals(List.of("acq-mux-2"), monitor.skipped());
    }

    @Test
    void aLinkThatReconnectedIsNamedUntilItSignsOnAgain() throws Exception {
        Rig rig = new Rig(true);
        rig.connect(0, new Date(1_700_000_000_000L));
        rig.connect(1, new Date(1_700_000_005_000L));
        rig.signOn(0);
        rig.signOn(1);
        PoolMonitor monitor = new PoolMonitor(rig.sp, true, rig.muxes);
        rig.disconnect(0);
        rig.connect(0, new Date(1_700_000_090_000L));     // the adaptor reconnects
        assertEquals(List.of("acq-mux-1"), monitor.skipped());
        rig.signOn(0);
        assertEquals(List.of(), monitor.skipped());
    }

    @Test
    void aMarkThatOnlyEqualsTheConnectionDoesNotCount() throws Exception {
        Rig rig = new Rig(true);
        rig.connect(0, new Date(1_700_000_000_000L));
        rig.connect(1, new Date(1_700_000_005_000L));
        rig.signOn(0);
        rig.sp.put("acq-mux-2.enabled", new Date(1_700_000_005_000L));   // equal, not the same
        PoolMonitor monitor = new PoolMonitor(rig.sp, true, rig.muxes);
        assertEquals(List.of("acq-mux-2"), monitor.skipped());
    }

    @Test
    void withoutCheckEnabledEveryConnectedLinkIsInUse() throws Exception {
        Rig rig = new Rig(false);
        rig.connect(0, new Date(1_700_000_000_000L));
        rig.connect(1, new Date(1_700_000_005_000L));
        PoolMonitor monitor = new PoolMonitor(rig.sp, false, rig.muxes);
        assertEquals(List.of(), monitor.skipped());
        rig.disconnect(0);
        assertEquals(List.of("acq-mux-1"), monitor.skipped());
    }

    // ---- test support: two real QMUXes and a real MUXPool over them, in this JVM ----

    private static final java.util.concurrent.atomic.AtomicInteger SPACES = new java.util.concurrent.atomic.AtomicInteger();

    static final class Rig {
        final LocalSpace<String, Object> sp;
        final QMUX[] muxes = new QMUX[2];
        final MUXPool pool;

        @SuppressWarnings("unchecked")
        Rig(boolean checkEnabled) throws Exception {
            int n = SPACES.incrementAndGet();
            String space = "tspace:monitor-" + n;
            sp = (LocalSpace<String, Object>) org.jpos.space.SpaceFactory.getSpace(space);
            for (int i = 0; i < 2; i++) {
                org.jdom2.Element e = new org.jdom2.Element("mux");
                e.addContent(new org.jdom2.Element("space").setText(space));
                e.addContent(new org.jdom2.Element("in").setText("acq-" + (i + 1) + "-receive"));
                e.addContent(new org.jdom2.Element("out").setText("acq-" + (i + 1) + "-send"));
                e.addContent(new org.jdom2.Element("ready").setText("acq-channel-" + (i + 1) + ".ready"));
                QMUX q = new QMUX();
                q.setName(i == 0 ? "acq-mux-1" : "acq-mux-2");
                q.setPersist(e);
                q.setConfiguration(new org.jpos.core.SimpleConfiguration());
                q.init();
                q.start();
                muxes[i] = q;
            }
            org.jdom2.Element p = new org.jdom2.Element("mux");
            p.addContent(new org.jdom2.Element("muxes").setText("acq-mux-1 acq-mux-2"));
            p.addContent(new org.jdom2.Element("strategy").setText("primary-secondary"));
            p.addContent(new org.jdom2.Element("space").setText(space));
            org.jpos.core.SimpleConfiguration cfg = new org.jpos.core.SimpleConfiguration();
            cfg.put("check-enabled", Boolean.toString(checkEnabled));
            pool = new MUXPool();
            pool.setName("acq-pool-" + n);
            pool.setPersist(p);
            pool.setConfiguration(cfg);
            pool.init();
            pool.start();
        }

        /** The adaptor of link i connects; this Date is its ready entry. */
        void connect(int i, Date when) { sp.put("acq-channel-" + (i + 1) + ".ready", when); }
        void disconnect(int i) { sp.inp("acq-channel-" + (i + 1) + ".ready"); }
        /** A sign-on manager's approved sign-on on link i's current connection. */
        void signOn(int i) { sp.put(muxes[i].getName() + ".enabled", sp.rdp("acq-channel-" + (i + 1) + ".ready")); }
    }
}
