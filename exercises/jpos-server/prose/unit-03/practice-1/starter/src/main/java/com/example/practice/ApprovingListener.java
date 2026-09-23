package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

/** Approves every request it is handed, answering through the request's own source. */
public final class ApprovingListener implements ISORequestListener {

    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        throw new UnsupportedOperationException("write process");
    }
}
