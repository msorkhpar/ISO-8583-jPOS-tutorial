package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.iso.MUX;

import java.util.function.UnaryOperator;

/**
 * The test lab's proxy. Testers' clients connect to it; it forwards each request to the
 * acquirer's certification host through {@code certHost}, and it can ask the lab's own simulator
 * ({@code simulator}: request in, answer out) instead.
 */
public class LabProxy implements ISORequestListener {
    private final MUX certHost;
    private final UnaryOperator<ISOMsg> simulator;
    private final long timeout;

    public LabProxy(MUX certHost, UnaryOperator<ISOMsg> simulator, long timeout) {
        this.certHost = certHost;
        this.simulator = simulator;
        this.timeout = timeout;
    }

    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        try {
            ISOMsg answer = answerFor(request);
            if (answer != null)
                source.send(answer);
        } catch (Exception e) {
            // logged in the real proxy
        }
        return true;
    }

    /** The answer the tester's client gets, or null when it must get none. */
    ISOMsg answerFor(ISOMsg request) throws ISOException {
        if (!certHost.isConnected())
            return simulator.apply(request);
        return certHost.request(request, timeout);
    }
}
