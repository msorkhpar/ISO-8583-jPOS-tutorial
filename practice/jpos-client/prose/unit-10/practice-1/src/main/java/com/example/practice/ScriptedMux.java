package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A host for client tests. The test scripts the next answers: a response code such as "00" or
 * "51", or SILENT for no answer. Once the script runs out, every request is approved.
 */
public class ScriptedMux implements MUX {
    public static final String SILENT = "silent";

    private final Deque<String> script = new ArrayDeque<>();
    private final List<ISOMsg> sent = new ArrayList<>();
    private boolean connected = true;

    /** Adds answers for the next requests, in order. */
    public synchronized ScriptedMux then(String... answers) {
        script.addAll(List.of(answers));
        return this;
    }

    public synchronized ScriptedMux down() {
        connected = false;
        return this;
    }

    /** Every message the client sent, in order. */
    public synchronized List<ISOMsg> sent() {
        return new ArrayList<>(sent);
    }

    synchronized String nextAnswer() {
        return script.isEmpty() ? "00" : script.poll();
    }

    @Override
    public synchronized ISOMsg request(ISOMsg m, long timeout) throws ISOException {
        // TODO: play the host for one request, following the script. A test must be able to
        //       check afterwards what the client sent, including requests nobody answered.
        throw new IllegalStateException("TODO: request");
    }

    /** The host's response to a request, carrying the given response code. */
    ISOMsg answer(ISOMsg request, String code) throws ISOException {
        // TODO: build the response the way a real host and QMUX would hand it to the client.
        throw new IllegalStateException("TODO: answer");
    }

    @Override
    public void request(ISOMsg m, long timeout, ISOResponseListener listener, Object handBack)
            throws ISOException {
        // TODO: the listener form of request: the client hears once, whatever happened.
        throw new IllegalStateException("TODO: request with listener");
    }

    @Override
    public synchronized void send(ISOMsg m) {
        sent.add(m);
    }

    @Override
    public synchronized boolean isConnected() {
        return connected;
    }
}
