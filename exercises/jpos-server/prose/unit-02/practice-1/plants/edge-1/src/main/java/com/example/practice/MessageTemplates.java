package com.example.practice;

import java.util.HashMap;
import java.util.Map;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

/** Named message templates, and the packager that puts their messages on the wire. */
public final class MessageTemplates {

    private final ISOPackager packager;
    private final Map<String, ISOMsg> templates = new HashMap<>();

    public MessageTemplates(ISOPackager packager) {
        this.packager = packager;
    }

    /** Remembers a template under a name. */
    public void define(String name, ISOMsg template) {
        templates.put(name, (ISOMsg) template.clone());
    }

    /** A new message that starts as a copy of the named template. */
    public ISOMsg newMessage(String name) {
        ISOMsg template = templates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("no template named " + name);
        }
        return template;
    }

    /** The bytes the packager makes of the message. */
    public byte[] pack(ISOMsg message) throws ISOException {
        message.setPackager(packager);
        return message.pack();
    }

    /** A new message read back from packed bytes. */
    public ISOMsg unpack(byte[] bytes) throws ISOException {
        ISOMsg message = new ISOMsg();
        message.setPackager(packager);
        message.unpack(bytes);
        return message;
    }
}
