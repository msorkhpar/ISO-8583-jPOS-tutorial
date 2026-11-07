package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.iso.IncomingListener;
import org.jpos.q2.Q2;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/** Runs the reader's checks against a correct CardStatus and against wrong ones, each in a real TransactionManager. */
class InFlightChecksTest {

    @Test
    void aCorrectCardStatusPassesEveryCheck() {
        assertDoesNotThrow(() -> InFlightChecks.checkAll(chains("correct")),
            "the checks must pass for a CardStatus that keeps every transaction to itself");
    }

    @Test
    void aRequestKeptInAFieldIsCaught() {
        assertCaught("request", "a CardStatus that keeps the request in a field while it asks the card service");
    }

    @Test
    void anAmountKeptInAFieldIsCaught() {
        assertCaught("amount", "a CardStatus that keeps only the amount in a field while it asks the card service");
    }

    private static void assertCaught(String variant, String what) {
        Throwable t = assertThrows(Throwable.class, () -> InFlightChecks.checkAll(chains(variant)),
            "the checks must fail for " + what);
        if (t instanceof IllegalStateException && String.valueOf(t.getMessage()).startsWith("TODO"))
            fail("a check is not written yet: " + t.getMessage());
    }

    // ---- the participant under check, and its card service ----

    /** The card service; the test's version can hold one lookup. */
    public static final class CardService {
        private final AtomicReference<CountDownLatch[]> next = new AtomicReference<>();
        private volatile CountDownLatch held = new CountDownLatch(1), release = new CountDownLatch(1);

        void holdNext() {
            held = new CountDownLatch(1);
            release = new CountDownLatch(1);
            next.set(new CountDownLatch[] { held, release });
        }

        String status(String card) throws InterruptedException {
            CountDownLatch[] hold = next.getAndSet(null);
            if (hold != null) {
                hold[0].countDown();
                if (!hold[1].await(20, TimeUnit.SECONDS))
                    throw new IllegalStateException("the held lookup was never released");
            }
            return "ACTIVE";
        }
    }

    /** Asks the card service about each purchase and answers it; "mode" selects a wrong version. */
    public static class CardStatus implements TransactionParticipant, Configurable {
        private CardService service;
        private String mode;
        private ISOMsg request;                                   // used by the "request" mode only
        private String amount;                                    // used by the "amount" mode only

        @Override
        public void setConfiguration(Configuration cfg) throws ConfigurationException {
            mode = cfg.get("mode");
            try {
                service = NameRegistrar.get(cfg.get("service"));
            } catch (NameRegistrar.NotFoundException e) {
                throw new ConfigurationException(e);
            }
        }

        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg mine = ctx.get(ContextConstants.REQUEST.toString());
            try {
                if (mode.equals("request")) request = mine;
                if (mode.equals("amount")) amount = mine.getString(4);
                String status = service.status(mine.getString(2));
                ISOMsg from = mode.equals("request") ? request : mine;
                ISOMsg response = (ISOMsg) from.clone();
                response.setResponseMTI();
                if (mode.equals("amount")) response.set(4, amount);
                response.set(39, "ACTIVE".equals(status) ? "00" : "62");
                ctx.put(ContextConstants.RESPONSE.toString(), response);
                return PREPARED | NO_JOIN;
            } catch (Exception e) {
                ctx.log(e);
                return ABORTED | NO_JOIN;
            }
        }
    }

    /** Last in the chain, after SendResponse: tells the terminal that the transaction has ended. */
    public static class Finished implements AbortParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return PREPARED | READONLY;
        }

        @Override
        public void commit(long id, Serializable context) {
            done(context);
        }

        @Override
        public void abort(long id, Serializable context) {
            done(context);
        }

        private static void done(Serializable context) {
            Object source = ((Context) context).get(ContextConstants.SOURCE.toString());
            if (source instanceof Terminal)
                ((Terminal) source).ended.countDown();
        }
    }

    // ---- the chains ----

    private static final List<String> VARIANTS = List.of("correct", "request", "amount");
    private static Q2 q2;

    /** One terminal, one request: collects what the server sends and learns when the transaction ended. */
    static final class Terminal implements ISOSource {
        final List<ISOMsg> received = new CopyOnWriteArrayList<>();
        final CountDownLatch ended = new CountDownLatch(1);

        @Override
        public void send(ISOMsg m) {
            received.add(m);
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        ISOMsg answer() throws InterruptedException {
            if (!ended.await(30, TimeUnit.SECONDS))
                throw new Error("the transaction did not end within 30 s");
            return received.isEmpty() ? null : received.get(0);
        }
    }

    static Supplier<InFlightChecks.Chain> chains(String variant) {
        return () -> {
            CardService service = NameRegistrar.getIfExists("cards." + variant);
            return new InFlightChecks.Chain() {
                @Override
                public ISOMsg send(ISOMsg request) throws Exception {
                    return start(request).answer();
                }

                @Override
                public InFlightChecks.Pending start(ISOMsg request) throws Exception {
                    IncomingListener listener = new IncomingListener();
                    SimpleConfiguration cfg = new SimpleConfiguration();
                    cfg.put("queue", "cards." + variant);
                    listener.setConfiguration(cfg);
                    Terminal terminal = new Terminal();
                    listener.process(terminal, (ISOMsg) request.clone());
                    return terminal::answer;
                }

                @Override
                public void holdNextLookup() {
                    service.holdNext();
                }

                @Override
                public void awaitHeldLookup() throws Exception {
                    if (!service.held.await(20, TimeUnit.SECONDS))
                        throw new Error("no lookup was held within 20 s");
                }

                @Override
                public void releaseLookup() {
                    service.release.countDown();
                }
            };
        };
    }

    @BeforeAll
    static void startQ2() throws Exception {
        Path deploy = Files.createTempDirectory("deploy");
        for (String v : VARIANTS) {
            NameRegistrar.register("cards." + v, new CardService());
            Files.writeString(deploy.resolve("30_" + v + ".xml"),
                "<txnmgr name='cards-" + v + "' class='org.jpos.transaction.TransactionManager'>\n"
              + "  <property name='queue' value='cards." + v + "'/>\n"
              + "  <property name='sessions' value='2'/>\n"
              + "  <property name='debug' value='false'/>\n"
              + "  <participant class='com.example.practice.InFlightChecksTest$CardStatus'>\n"
              + "    <property name='mode' value='" + v + "'/>\n"
              + "    <property name='service' value='cards." + v + "'/>\n"
              + "  </participant>\n"
              + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
              + "  <participant class='com.example.practice.InFlightChecksTest$Finished'/>\n"
              + "</txnmgr>\n");
        }
        q2 = new Q2(deploy.toString());
        q2.start();
        for (String v : VARIANTS)
            assertNotNull(NameRegistrar.get("cards-" + v, 30000), "TransactionManager cards-" + v + " did not start");
    }

    @AfterAll
    static void stopQ2() {
        q2.shutdown(true);
    }
}
