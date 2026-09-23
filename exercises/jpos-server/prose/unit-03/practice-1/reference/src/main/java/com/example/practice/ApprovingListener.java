package com.example.practice;

import java.io.IOException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

/** Approves every request it is handed, answering through the request's own source. */
public final class ApprovingListener implements ISORequestListener {

    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        try {
            ISOMsg reply = (ISOMsg) request.clone();
            reply.setResponseMTI();
            reply.set(39, "00");
            source.send(reply);
            return true;
        } catch (ISOException | IOException e) {
            return false;
        }
    }
}
