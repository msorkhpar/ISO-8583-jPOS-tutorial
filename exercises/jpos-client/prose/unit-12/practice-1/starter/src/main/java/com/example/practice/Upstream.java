package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;

/**
 * The gateway's side of the upstream link. Terminals send their requests to {@link #forward};
 * the gateway relays each one to the acquirer's host through one QMUX, as one terminal.
 */
public class Upstream {
    private final MUX mux;
    private final String gatewayId;

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
        // TODO: On this link the gateway is the sender, and the STAN is the sender's number for
        //       the message. Today the terminal's STAN goes up unchanged, which is what collides
        //       at lunchtime. One line here, using nextStan().
        return up;
    }

    /** The response as the terminal expects it: an answer to the request it sent. */
    ISOMsg forTerminal(ISOMsg response, ISOMsg fromTerminal) throws ISOException {
        // TODO: The host answered the gateway's message. The terminal matches responses to its
        //       requests the same way QMUX does, by its own terminal ID and STAN. Return a copy
        //       of the response that the terminal will recognise. Leave `response` unchanged.
        throw new IllegalStateException("TODO: forTerminal");
    }

    /** The STAN of the next message the gateway sends on this link. */
    String nextStan() {
        // TODO: Six digits, never 000000, and never the same twice while a message could still
        //       be waiting. Several terminals call forward() at the same time. Add the field you
        //       need; ISOUtil.zeropad may help.
        throw new IllegalStateException("TODO: nextStan");
    }
}
