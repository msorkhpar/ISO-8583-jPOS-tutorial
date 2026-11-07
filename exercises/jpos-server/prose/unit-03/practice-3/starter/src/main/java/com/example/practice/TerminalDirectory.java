package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * First in the QServer's listener chain. Learns which connection each terminal uses, so that the
 * server can send a terminal the messages it starts itself (key changes, sign-offs, echoes).
 */
public class TerminalDirectory implements ISORequestListener {
    private final Map<String, ISOSource> connections = new ConcurrentHashMap<>();

    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        // TODO: Every message from every connection passes through here first. Learn from it what
        //       send() will need, and leave the message to the listeners after this one.
        //       (three lines)
        throw new IllegalStateException("TODO: process");
    }

    /**
     * Sends m to the terminal named in its field 41.
     * @return true if it was sent, false if that terminal can't be reached now
     */
    public boolean send(ISOMsg m) throws IOException, ISOException {
        // TODO: Use what process() learned. A terminal that can't be reached now is not an error
        //       here: say so with the return value. (about five lines)
        throw new IllegalStateException("TODO: send");
    }
}
