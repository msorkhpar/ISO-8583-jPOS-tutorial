package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The gateway's side of the upstream link. Terminals send their requests to {@link #forward};
 * the gateway relays each one to the acquirer's host through one QMUX, as one terminal.
 */
public class Upstream {
    private final MUX mux;
    private final String gatewayId;
    private final AtomicLong sent = new AtomicLong();

    public Upstream(MUX mux, String gatewayId) {
        this.mux = mux;
        this.gatewayId = gatewayId;
    }

    /** Relays a terminal's request and returns the response the terminal should get, or null. */
    public ISOMsg forward(ISOMsg fromTerminal) throws ISOException {
        ISOMsg up = upstreamCopy(fromTerminal);
        ISOMsg response = mux.request(up, 30000);
        return response == null ? null : forTerminal(response, fromTerminal);
    }

    /** The message the host receives: the terminal's request, sent by the gateway. */
    ISOMsg upstreamCopy(ISOMsg fromTerminal) throws ISOException {
        ISOMsg up = (ISOMsg) fromTerminal.clone();
        up.set(41, gatewayId);
        up.set(11, nextStan());
        return up;
    }

    /** The response as the terminal expects it: an answer to the request it sent. */
    ISOMsg forTerminal(ISOMsg response, ISOMsg fromTerminal) throws ISOException {
        ISOMsg down = (ISOMsg) response.clone();
        down.set(11, fromTerminal.getString(11));
        down.set(41, fromTerminal.getString(41));
        return down;
    }

    /** The STAN of the next message the gateway sends on this link. */
    String nextStan() {
        long n = sent.incrementAndGet();
        return ISOUtil.zeropad(n % 1000000, 6);
    }
}
