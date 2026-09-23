package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

/** Named message templates, and the packager that puts their messages on the wire. */
public final class MessageTemplates {

    public MessageTemplates(ISOPackager packager) {
        throw new UnsupportedOperationException("write MessageTemplates");
    }

    /** Remembers a template under a name. */
    public void define(String name, ISOMsg template) {
        throw new UnsupportedOperationException("write define");
    }

    /** A new message that starts as a copy of the named template. */
    public ISOMsg newMessage(String name) {
        throw new UnsupportedOperationException("write newMessage");
    }

    /** The bytes the packager makes of the message. */
    public byte[] pack(ISOMsg message) throws ISOException {
        throw new UnsupportedOperationException("write pack");
    }

    /** A new message read back from packed bytes. */
    public ISOMsg unpack(byte[] bytes) throws ISOException {
        throw new UnsupportedOperationException("write unpack");
    }
}
