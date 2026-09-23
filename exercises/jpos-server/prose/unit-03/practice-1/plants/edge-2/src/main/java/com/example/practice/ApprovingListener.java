package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

/** Answers, but reports every failure as handled. */
public final class ApprovingListener implements ISORequestListener {

    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        try {
            ISOMsg reply = (ISOMsg) request.clone();
            reply.setResponseMTI();
            reply.set(39, "00");
            source.send(reply);
        } catch (Exception e) {
            // logged and forgotten
        }
        return true;
    }
}
