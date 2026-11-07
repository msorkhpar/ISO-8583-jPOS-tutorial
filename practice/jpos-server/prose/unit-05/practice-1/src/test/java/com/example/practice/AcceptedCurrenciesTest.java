package com.example.practice;

import org.jpos.core.ConfigurationException;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.q2.Q2;
import org.jpos.space.SpaceFactory;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Runs AcceptedCurrencies in the brand groups of a real TransactionManager, deployed by Q2 in this JVM. */
class AcceptedCurrenciesTest {
    private static final String VISA = "4111111111111111", MASTERCARD = "5555555555554444";

    @Test
    void purchasesInEuroAreApprovedForBothBrands() throws Exception {
        assertEquals("00", answer(VISA, "978", "000301").getString(39));
        assertEquals("00", answer(MASTERCARD, "978", "000302").getString(39));
    }

    @Test
    void aMastercardPurchaseInDollarsIsDeclinedAndAnswered() throws Exception {
        ISOMsg r = answer(MASTERCARD, "840", "000303");
        assertEquals("0210", r.getMTI());
        assertEquals("57", r.getString(39), "the mastercard group is configured with rc 57");
        assertEquals("000303", r.getString(11), "the answer is the purchase's own");
    }

    @Test
    void eachBrandKeepsItsOwnContract() throws Exception {
        assertEquals("00", answer(VISA, "840", "000304").getString(39),
            "the visa group accepts 978, 840 and 826");
        assertEquals("00", answer(VISA, "826", "000305").getString(39));
        assertEquals("58", answer(VISA, "392", "000306").getString(39),
            "the visa group declines with the default code");
        assertEquals("57", answer(MASTERCARD, "826", "000307").getString(39),
            "the mastercard group accepts 978 only");
    }

    @Test
    void aContractWithoutCurrenciesDoesNotDeploy() {
        SimpleConfiguration none = new SimpleConfiguration();
        none.put("rc", "58");
        assertThrows(ConfigurationException.class, () -> new AcceptedCurrencies().setConfiguration(none));
        SimpleConfiguration misspelt = new SimpleConfiguration();
        misspelt.put("currency", "978");
        assertThrows(ConfigurationException.class, () -> new AcceptedCurrencies().setConfiguration(misspelt));
    }

    // ---- requests ----

    private static ISOMsg answer(String pan, String currency, String stan) throws Exception {
        assertNotNull(txnmgr, "the TransactionManager did not start");
        ISOMsg m = new ISOMsg("0200");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, "000000001875");
        m.set(11, stan);
        m.set(41, "29110042");
        m.set(49, currency);
        BlockingQueue<ISOMsg> sent = new LinkedBlockingQueue<>();
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        ctx.put(ContextConstants.SOURCE.toString(), new ISOSource() {
            @Override public void send(ISOMsg r) { sent.add(r); }
            @Override public boolean isConnected() { return true; }
        });
        SpaceFactory.getSpace().out("practice.currencies", ctx);
        ISOMsg r = sent.poll(20, TimeUnit.SECONDS);
        assertNotNull(r, "the terminal got no answer");
        return r;
    }

    // ---- test support: Q2 in this JVM, deploying a TransactionManager with two brand groups ----

    private static Q2 q2;
    private static Object txnmgr;

    @BeforeAll
    static void startQ2() throws Exception {
        Path deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-currencies' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='practice.currencies'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SelectDestination'>\n"
          + "    <endpoint destination='visa'>4</endpoint>\n"
          + "    <endpoint destination='mastercard'>51..55 2221..2720</endpoint>\n"
          + "  </participant>\n"
          + "  <participant class='org.jpos.transaction.participant.Switch'>\n"
          + "    <property name='txnname' value='DESTINATION'/>\n"
          + "    <property name='visa' value='visa'/>\n"
          + "    <property name='mastercard' value='mastercard'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.AcceptedCurrenciesTest$Approve'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <group name='visa'>\n"
          + "    <participant class='com.example.practice.AcceptedCurrencies'>\n"
          + "      <property name='currencies' value='978, 840 826'/>\n"
          + "    </participant>\n"
          + "  </group>\n"
          + "  <group name='mastercard'>\n"
          + "    <participant class='com.example.practice.AcceptedCurrencies'>\n"
          + "      <property name='currencies' value='978'/>\n"
          + "      <property name='rc' value='57'/>\n"
          + "    </participant>\n"
          + "  </group>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        txnmgr = NameRegistrar.get("practice-currencies", 15000);
    }

    @AfterAll
    static void stopQ2() {
        if (q2 != null) q2.shutdown(true);
    }

    /** Stands in for the authorizer: approves whatever reaches it. */
    public static class Approve implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
            ctx.put(ContextConstants.RESPONSE.toString(), AcceptedCurrencies.answer(request, "00"));
            return PREPARED | NO_JOIN;
        }
    }
}
