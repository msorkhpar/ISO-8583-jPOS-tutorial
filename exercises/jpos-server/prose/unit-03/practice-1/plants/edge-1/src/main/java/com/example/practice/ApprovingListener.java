package com.example.practice;

import java.io.IOException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

/** Answers by turning the request itself into the reply. */
public final class ApprovingListener implements ISORequestListener {

    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        try {
            request.setResponseMTI();
            request.set(39, "00");
            source.send(request);
            return true;
        } catch (ISOException | IOException e) {
            return false;
        }
    }
}
