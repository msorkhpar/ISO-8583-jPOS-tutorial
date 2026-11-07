package com.example.practice;

import org.jpos.q2.QBeanSupport;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinServiceTest {
    private static final String BLOCK = "0412AC6BF3E1D2A9";
    private Deploy deploy;

    @AfterEach
    void stopQ2() {
        if (deploy != null)
            deploy.close();
    }

    static String hsm(String name) {
        return "<hsm name='" + name + "' class='com.example.practice.PinServiceTest$FakeHsm'/>";
    }

    static String pinService(String name, String hsm, long wait) {
        return "<pin-service name='" + name + "' class='com.example.practice.PinService'>"
                + "<property name='hsm' value='" + hsm + "'/>"
                + "<property name='wait' value='" + wait + "'/></pin-service>";
    }

    @Test
    void translatesThroughAnHsmThatIsAlreadyRunning() throws Exception {
        deploy = new Deploy().file("12_hsm.xml", hsm("hsm-main")).start();
        deploy.addWhileRunning("25_pin.xml", pinService("pin-main", "hsm-main", 1000));
        deploy.awaitState("pin-main", "Started");
        PinService pins = NameRegistrar.get("pin-main");
        FakeHsm hsm = NameRegistrar.get("hsm-main");
        assertEquals(hsm.expected(BLOCK), pins.translate(BLOCK));
    }

    @Test
    void startsWhenDeployedInTheSamePassAsTheHsm() throws Exception {
        deploy = new Deploy()
                .file("12_hsm.xml", hsm("hsm-pass"))
                .file("25_pin.xml", pinService("pin-pass", "hsm-pass", 1000))
                .start();
        assertEquals("Started", deploy.state("pin-pass"), "after a restart both files are in the first pass");
        PinService pins = NameRegistrar.get("pin-pass");
        FakeHsm hsm = NameRegistrar.get("hsm-pass");
        assertEquals(hsm.expected(BLOCK), pins.translate(BLOCK));
    }

    @Test
    void aMissingHsmLeavesTheServiceFailedUntilStartedAgain() throws Exception {
        deploy = new Deploy().file("25_pin.xml", pinService("pin-alone", "hsm-late", 300)).start();
        assertEquals("Failed", deploy.state("pin-alone"), "an operator sees that the PIN service is not working");
        deploy.addWhileRunning("12_hsm.xml", hsm("hsm-late"));
        assertTrue(NameRegistrar.get("hsm-late", 30_000) != null, "the HSM was deployed");
        deploy.invoke("pin-alone", "start");
        assertEquals("Started", deploy.state("pin-alone"), "started again once the HSM is up");
        PinService pins = NameRegistrar.get("pin-alone");
        FakeHsm hsm = NameRegistrar.get("hsm-late");
        assertEquals(hsm.expected(BLOCK), pins.translate(BLOCK));
    }

    @Test
    void aRedeployedHsmIsUsed() throws Exception {
        deploy = new Deploy()
                .file("12_hsm.xml", hsm("hsm-redeploy"))
                .file("25_pin.xml", pinService("pin-redeploy", "hsm-redeploy", 1000))
                .start();
        PinService pins = NameRegistrar.get("pin-redeploy");
        FakeHsm before = NameRegistrar.get("hsm-redeploy");
        assertEquals(before.expected(BLOCK), pins.translate(BLOCK));
        deploy.touch("12_hsm.xml");
        FakeHsm after = deploy.awaitReplaced("hsm-redeploy", before);
        assertNotSame(before, after);
        assertEquals(after.expected(BLOCK), pins.translate(BLOCK), "the HSM deployed now does the work");
    }

    /** An HSM driver as a QBean: registers itself when it starts, refuses work once stopped. */
    public static class FakeHsm extends QBeanSupport implements PinService.Hsm {
        private static final AtomicInteger INSTANCES = new AtomicInteger();
        private final int instance = INSTANCES.incrementAndGet();
        private volatile boolean up;

        @Override
        protected void startService() {
            up = true;
            NameRegistrar.register(getName(), this);
        }

        @Override
        protected void stopService() {
            up = false;
            NameRegistrar.unregister(getName());
        }

        @Override
        public String translate(String pinBlock) {
            if (!up)
                throw new IllegalStateException("HSM instance " + instance + " has been stopped");
            return expected(pinBlock);
        }

        String expected(String pinBlock) {
            return "hsm" + instance + ":" + new StringBuilder(pinBlock).reverse();
        }
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

        /** Written elsewhere and moved in, so Q2 never sees half a file. */
        void addWhileRunning(String name, String xml) throws java.io.IOException {
            java.nio.file.Path staging = java.nio.file.Files.createTempDirectory(dir.getParent(), "staging");
            java.nio.file.Path tmp = java.nio.file.Files.writeString(staging.resolve(name), xml);
            java.nio.file.Files.move(tmp, dir.resolve(name), java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        }

        void touch(String name) throws java.io.IOException {
            java.nio.file.Path f = dir.resolve(name);
            long later = java.nio.file.Files.getLastModifiedTime(f).toMillis() + 5_000;
            java.nio.file.Files.setLastModifiedTime(f, java.nio.file.attribute.FileTime.fromMillis(later));
        }

        Deploy start() {
            q2 = new org.jpos.q2.Q2(dir.toString());
            q2.start();
            if (!q2.ready(30_000))
                throw new AssertionError("Q2 did not finish its first pass");
            return this;
        }

        String state(String bean) throws Exception {
            javax.management.MBeanServer jmx = q2.getMBeanServer();
            javax.management.ObjectName on = new javax.management.ObjectName("Q2:type=qbean,service=" + bean);
            return jmx.isRegistered(on) ? (String) jmx.getAttribute(on, "StateAsString") : "-";
        }

        void invoke(String bean, String operation) throws Exception {
            q2.getMBeanServer().invoke(new javax.management.ObjectName("Q2:type=qbean,service=" + bean),
                    operation, null, null);
        }

        void awaitState(String bean, String wanted) throws Exception {
            long until = System.currentTimeMillis() + 30_000;
            while (!wanted.equals(state(bean))) {
                if (System.currentTimeMillis() > until)
                    throw new AssertionError(bean + " is " + state(bean) + ", not " + wanted);
                Thread.sleep(20);
            }
        }

        <T> T awaitReplaced(String name, T old) throws InterruptedException {
            long until = System.currentTimeMillis() + 30_000;
            while (true) {
                T now = NameRegistrar.getIfExists(name);
                if (now != null && now != old)
                    return now;
                if (System.currentTimeMillis() > until)
                    throw new AssertionError(name + " was not redeployed");
                Thread.sleep(20);
            }
        }

        @Override
        public void close() {
            q2.shutdown(true);
        }
    }
}
