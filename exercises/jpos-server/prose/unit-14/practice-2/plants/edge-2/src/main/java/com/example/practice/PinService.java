package com.example.practice;

import org.jpos.core.annotation.Config;
import org.jpos.q2.QBeanSupport;
import org.jpos.util.NameRegistrar;

/**
 * Translates PIN blocks from the terminals' key to the issuer's, through the HSM driver: a QBean
 * of its own that registers an {@link Hsm} under its name when it starts. Deployed from a
 * descriptor such as:
 *
 * <pre>
 * &lt;pin-service name="pin-service" class="com.example.practice.PinService"&gt;
 *   &lt;property name="hsm" value="hsm"/&gt;
 *   &lt;property name="wait" value="1000"/&gt;
 * &lt;/pin-service&gt;
 * </pre>
 */
public class PinService extends QBeanSupport {
    /** What the HSM driver registers. */
    public interface Hsm {
        String translate(String pinBlock);
    }

    @Config("hsm")
    private String hsmName = "hsm";               // the name the HSM driver registers under
    @Config("wait")
    private long wait = 1000L;                    // ms the start may wait for it

    @Override
    protected void startService() {
        awaitHsm();
        NameRegistrar.register(getName(), this);
    }

    @Override
    protected void stopService() {
        NameRegistrar.unregister(getName());
    }

    /** Called when the service starts, before anything uses it. */
    void awaitHsm() {
        if (NameRegistrar.get(hsmName, wait) == null)
            getLog().warn("HSM '" + hsmName + "' is not registered yet");
    }

    /** The HSM to use for this call. */
    Hsm hsm() {
        Hsm hsm = NameRegistrar.getIfExists(hsmName);
        if (hsm == null)
            throw new IllegalStateException("HSM '" + hsmName + "' is not registered");
        return hsm;
    }

    public String translate(String pinBlock) {
        if (!running())
            throw new IllegalStateException("the PIN service is not running");
        return hsm().translate(pinBlock);
    }
}
