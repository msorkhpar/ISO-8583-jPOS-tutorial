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
    private final List<Held> held = new ArrayList<>();

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
        synchronized (held) {
            held.add(0, new Held(client, answer));
        }
    }

    /**
     * Sends the answers held so far, and returns how many reached a client. Called from the
     * test's thread while sessions keep calling process.
     */
    public int release() {
        List<Held> now;
        synchronized (held) {
            now = new ArrayList<>(held);
            held.clear();
        }
        int sent = 0;
        for (Held h : now) {
            try {
                if (h.client().isConnected()) {
                    h.client().send(h.answer());
                    sent++;
                }
            } catch (Exception gone) {
                // the client left between the check and the send
            }
        }
        return sent;
    }
}
