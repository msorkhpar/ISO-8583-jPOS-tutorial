package com.example.practice;

import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandInLimitTest {
    private Deploy deploy;

    @AfterEach
    void stopQ2() {
        if (deploy != null)
            deploy.close();
    }

    /** A descriptor for StandInLimit; props are name, value, name, value... */
    static String standin(String name, String... props) {
        StringBuilder sb = new StringBuilder("<standin name='" + name + "' class='com.example.practice.StandInLimit'>");
        for (int i = 0; i < props.length; i += 2)
            sb.append("<property name='").append(props[i]).append("' value='").append(props[i + 1]).append("'/>");
        return sb.append("</standin>").toString();
    }

    @Test
    void aConfiguredLimitApprovesUpToItsAmount() throws Exception {
        deploy = new Deploy()
                .file("40_standin_eur.xml", standin("standin-eur", "max-amount", "7500", "currency", "978"))
                .start();
        assertEquals("Started", deploy.state("standin-eur"));
        StandInLimit limit = NameRegistrar.get("standin-eur");
        assertTrue(limit.allows("978", 7500));
        assertFalse(limit.allows("978", 7501));
        assertFalse(limit.allows("840", 100), "another currency is not covered");
    }

    @Test
    void aLimitNobodySetDoesNotRun() throws Exception {
        deploy = new Deploy()
                .file("40_standin_ok.xml", standin("standin-ok", "max-amount", "12000", "currency", "978"))
                .file("41_standin_unset.xml", standin("standin-unset", "max-amount", "${STANDIN_MAX}", "currency", "978"))
                .start();
        assertEquals("Started", deploy.state("standin-ok"));
        assertEquals("-", deploy.state("standin-unset"), "a limit nobody set must not be running");
        assertTrue(deploy.renamedBad("41_standin_unset.xml"), "the operator sees 41_standin_unset.xml.BAD");
    }

    @Test
    void aMisspeltLimitShowsInTheDeployDirectory() throws Exception {
        deploy = new Deploy()
                .file("40_standin_ok.xml", standin("standin-ok", "max-amount", "12000", "currency", "978"))
                .file("42_standin_typo.xml", standin("standin-typo", "max_amount", "3000", "currency", "978"))
                .start();
        assertEquals("Started", deploy.state("standin-ok"));
        assertTrue(deploy.renamedBad("42_standin_typo.xml"),
                "listing deploy/ shows 42_standin_typo.xml.BAD, not a file that looks deployed");
        assertEquals("-", deploy.state("standin-typo"));
    }

    @Test
    void aLimitThatIsNotWholeMinorUnitsDoesNotRun() throws Exception {
        deploy = new Deploy()
                .file("40_standin_ok.xml", standin("standin-ok", "max-amount", "12000", "currency", "978"))
                .file("43_standin_decimal.xml", standin("standin-decimal", "max-amount", "75.00", "currency", "978"))
                .start();
        assertEquals("Started", deploy.state("standin-ok"));
        assertTrue(deploy.renamedBad("43_standin_decimal.xml"), "the operator sees 43_standin_decimal.xml.BAD");
        assertEquals("-", deploy.state("standin-decimal"));
    }

    @Test
    void theCurrencyIsUsDollarsWhenItsVariableIsUnset() throws Exception {
        deploy = new Deploy()
                .file("44_standin_usd.xml", standin("standin-usd", "max-amount", "2500", "currency", "${STANDIN_CURRENCY}"))
                .start();
        assertEquals("Started", deploy.state("standin-usd"));
        StandInLimit limit = NameRegistrar.get("standin-usd");
        assertTrue(limit.allows("840", 2500));
        assertFalse(limit.allows("978", 100));
    }

    // ---- test support: a real Q2 in this JVM, on a scratch deploy directory ----

    static final class Deploy implements AutoCloseable {
        final java.nio.file.Path dir;
        private org.jpos.q2.Q2 q2;

        Deploy() throws java.io.IOException {
            dir = java.nio.file.Files.createTempDirectory("deploy");
        }

        Deploy file(String name, String xml) throws java.io.IOException {
            java.nio.file.Files.writeString(dir.resolve(name), xml);
            return this;
        }

        Deploy start() {
            q2 = new org.jpos.q2.Q2(dir.toString());
            q2.start();
            if (!q2.ready(30_000))
                throw new AssertionError("Q2 did not finish its first pass");
            return this;
        }

        /** The QBean's state as JMX shows it, or "-" when nothing is registered under the name. */
        String state(String bean) throws Exception {
            javax.management.MBeanServer jmx = q2.getMBeanServer();
            javax.management.ObjectName on = new javax.management.ObjectName("Q2:type=qbean,service=" + bean);
            return jmx.isRegistered(on) ? (String) jmx.getAttribute(on, "StateAsString") : "-";
        }

        boolean renamedBad(String file) {
            return java.nio.file.Files.exists(dir.resolve(file + ".BAD"))
                    && !java.nio.file.Files.exists(dir.resolve(file));
        }

        @Override
        public void close() {
            q2.shutdown(true);
        }
    }
}
