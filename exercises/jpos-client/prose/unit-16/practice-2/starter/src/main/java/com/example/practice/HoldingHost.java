package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A host simulator for automated tests: it approves every request, but holds back the answer to
 * the STANs it was given until the test calls {@link #release()}.
 */
public class HoldingHost implements ISORequestListener {
    record Held(ISOSource client, ISOMsg answer) { }

    private final Set<String> heldStans;

    public HoldingHost(Set<String> heldStans) {
        this.heldStans = Set.copyOf(heldStans);
    }

    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        try {
            if (!m.isRequest())
                return false;
            ISOMsg answer = (ISOMsg) m.clone();
            answer.setResponseMTI();
            answer.set(39, "00");
            if (heldStans.contains(m.getString(11)))
                hold(source, answer);
            else
                source.send(answer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Keeps an answer back until the test releases it. Called on the client's session thread. */
    void hold(ISOSource client, ISOMsg answer) {
        // TODO: Keep the answer until release(). Remember which thread is running this.
        //       Add the field you need.
        throw new IllegalStateException("TODO: hold");
    }

    /**
     * Sends the answers held so far, and returns how many reached a client. Called from the
     * test's thread while sessions keep calling process.
     */
    public int release() {
        // TODO: Answer what was held, the way the real host would have answered it.
        throw new IllegalStateException("TODO: release");
    }
}
