package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOException;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/** Runs the reader's checks against a correct HoldFunds and against wrong ones, each in a real TransactionManager. */
class HoldFundsChecksTest {

    @Test
    void aCorrectHoldFundsPassesEveryCheck() {
        assertDoesNotThrow(() -> HoldFundsChecks.checkAll(chains("correct")),
            "the checks must pass for a HoldFunds that does everything right");
    }

    @Test
    void aHoldThatTheIssuersDeclineNeverReturnsIsCaught() {
        assertCaught("nojoin", "a HoldFunds that keeps the money of a purchase the issuer declined");
    }

    @Test
    void aRefusalThatAddsMoneyToTheCardIsCaught() {
        assertCaught("unchecked", "a HoldFunds whose own refusal adds the amount to the card");
    }

    @Test
    void aRefusalThatLeavesTheTerminalWithoutAnAnswerIsCaught() {
        assertCaught("silent", "a HoldFunds that refuses a purchase without answering the terminal");
    }

    private static void assertCaught(String variant, String what) {
        Throwable t = assertThrows(Throwable.class, () -> HoldFundsChecks.checkAll(chains(variant)),
            "the checks must fail for " + what);
        if (t instanceof IllegalStateException && String.valueOf(t.getMessage()).startsWith("TODO"))
            fail("a check is not written yet: " + t.getMessage());
    }

    // ---- the participants under check ----

    /** Holds the amount on the card in prepare and gives it back in abort; "mode" selects a wrong version. */
    public static class HoldFunds implements TransactionParticipant, Configurable {
        private static final String HELD = "HELD";
        private Map<String, Long> balances;
        private String mode;

        @Override
        public void setConfiguration(Configuration cfg) throws ConfigurationException {
            mode = cfg.get("mode");
            try {
                balances = NameRegistrar.get(cfg.get("balances"));
            } catch (NameRegistrar.NotFoundException e) {
                throw new ConfigurationException(e);
            }
        }

        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
            try {
                String card = request.getString(2);
                long amount = Long.parseLong(request.getString(4));
                synchronized (balances) {
                    long balance = balances.getOrDefault(card, 0L);
                    if (balance < amount) {
                        if (!mode.equals("silent"))
                            ctx.put(ContextConstants.RESPONSE.toString(), reply(request, "51"));
                        return mode.equals("unchecked") ? ABORTED : ABORTED | NO_JOIN;
                    }
                    balances.put(card, balance - amount);
                }
                ctx.put(HELD, amount);
                return mode.equals("nojoin") ? PREPARED | NO_JOIN : PREPARED;
            } catch (Exception e) {
                ctx.log(e);
                return ABORTED | NO_JOIN;
            }
        }

        @Override
        public void abort(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
            Long held = ctx.get(HELD);
            if (mode.equals("unchecked"))
                held = Long.parseLong(request.getString(4));
            if (held != null)
                balances.merge(request.getString(2), held, Long::sum);
        }
    }

    /** The issuer: answers what the test set, approving with 00. */
    public static class Issuer implements TransactionParticipant, Configurable {
        private AtomicReference<String> answer;

        @Override
        public void setConfiguration(Configuration cfg) throws ConfigurationException {
            try {
                answer = NameRegistrar.get(cfg.get("answer"));
            } catch (NameRegistrar.NotFoundException e) {
                throw new ConfigurationException(e);
            }
        }

        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
            try {
                String rc = answer.get();
                ISOMsg response = reply(request, rc);
                ctx.put(ContextConstants.RESPONSE.toString(), response);
                if (!"00".equals(rc))
                    return ABORTED | NO_JOIN;
                response.set(38, "A10001");
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

    static ISOMsg reply(ISOMsg request, String rc) throws ISOException {
        ISOMsg r = (ISOMsg) request.clone();
        r.setResponseMTI();
        r.set(39, rc);
        return r;
    }

    // ---- the chains ----

    private static final List<String> VARIANTS = List.of("correct", "nojoin", "unchecked", "silent");
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
    }

    static Supplier<HoldFundsChecks.Chain> chains(String variant) {
        return () -> {
            Map<String, Long> balances = NameRegistrar.getIfExists("balances." + variant);
            AtomicReference<String> issuer = NameRegistrar.getIfExists("issuer." + variant);
            balances.clear();                                    // the object HoldFunds holds
            issuer.set("00");
            return new HoldFundsChecks.Chain() {
                @Override
                public void open(String card, long balance) {
                    balances.put(card, balance);
                }

                @Override
                public long balance(String card) {
                    return balances.getOrDefault(card, 0L);
                }

                @Override
                public void issuerAnswers(String responseCode) {
                    issuer.set(responseCode);
                }

                @Override
                public ISOMsg send(ISOMsg request) throws Exception {
                    IncomingListener listener = new IncomingListener();
                    SimpleConfiguration cfg = new SimpleConfiguration();
                    cfg.put("queue", "hold." + variant);
                    listener.setConfiguration(cfg);
                    Terminal terminal = new Terminal();
                    listener.process(terminal, (ISOMsg) request.clone());
                    if (!terminal.ended.await(20, TimeUnit.SECONDS))
                        throw new Error("the transaction did not end within 20 s");
                    return terminal.received.isEmpty() ? null : terminal.received.get(0);
                }
            };
        };
    }

    @BeforeAll
    static void startQ2() throws Exception {
        Path deploy = Files.createTempDirectory("deploy");
        for (String v : VARIANTS) {
            NameRegistrar.register("balances." + v, new ConcurrentHashMap<String, Long>());
            NameRegistrar.register("issuer." + v, new AtomicReference<>("00"));
            Files.writeString(deploy.resolve("30_" + v + ".xml"),
                "<txnmgr name='hold-" + v + "' class='org.jpos.transaction.TransactionManager'>\n"
              + "  <property name='queue' value='hold." + v + "'/>\n"
              + "  <property name='sessions' value='2'/>\n"
              + "  <property name='debug' value='false'/>\n"
              + "  <participant class='com.example.practice.HoldFundsChecksTest$HoldFunds'>\n"
              + "    <property name='mode' value='" + v + "'/>\n"
              + "    <property name='balances' value='balances." + v + "'/>\n"
              + "  </participant>\n"
              + "  <participant class='com.example.practice.HoldFundsChecksTest$Issuer'>\n"
              + "    <property name='answer' value='issuer." + v + "'/>\n"
              + "  </participant>\n"
              + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
              + "  <participant class='com.example.practice.HoldFundsChecksTest$Finished'/>\n"
              + "</txnmgr>\n");
        }
        q2 = new Q2(deploy.toString());
        q2.start();
        for (String v : VARIANTS)
            assertNotNull(NameRegistrar.get("hold-" + v, 30000), "TransactionManager hold-" + v + " did not start");
    }

    @AfterAll
    static void stopQ2() {
        q2.shutdown(true);
    }
}
