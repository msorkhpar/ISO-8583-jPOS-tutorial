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
        if (m.hasField(41))
            connections.put(m.getString(41), source);
        return false;
    }

    /**
     * Sends m to the terminal named in its field 41.
     * @return true if it was sent, false if that terminal can't be reached now
     */
    public boolean send(ISOMsg m) throws IOException, ISOException {
        ISOSource source = connections.get(m.getString(41));
        if (source == null)
            return false;
        source.send(m);
        return true;
    }
}
