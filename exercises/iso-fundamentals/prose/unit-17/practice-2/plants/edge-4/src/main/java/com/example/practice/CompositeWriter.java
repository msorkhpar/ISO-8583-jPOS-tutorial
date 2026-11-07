package com.example.practice;

import java.io.ByteArrayOutputStream;
import org.jpos.tlv.TLVList;
import java.util.List;

/** Writes TLV datasets into the bytes of an ISO 8583:2023 composite field. */
public final class CompositeWriter {

    /** One BER-TLV element: a tag such as 0xDF8115 and its value. */
    public record Element(int tag, byte[] value) {
    }

    /** One TLV dataset: its identifier and its elements, in the order to write them. */
    public record Dataset(int id, List<Element> elements) {
    }

    private CompositeWriter() {
    }

    /** The composite field: every dataset, in the order given. */
    public static byte[] write(List<Dataset> datasets) {
        ByteArrayOutputStream field = new ByteArrayOutputStream();
        for (Dataset ds : datasets)
            field.writeBytes(envelope(ds.id(), content(ds.elements())));
        return field.toByteArray();
    }

    /** The content of a TLV dataset: its elements, in the order given. */
    static byte[] content(List<Element> elements) {
        TLVList tlv = new TLVList();
        for (Element e : elements)
            tlv.append(e.tag(), e.value());
        return tlv.pack();
    }

    /** One TLV dataset as it goes into the field. */
    static byte[] envelope(int id, byte[] content) {
        if (id < 0x01 || id > 0x70)
            throw new IllegalArgumentException("not a TLV dataset identifier: " + id);
        if (content.length > 0xFFFF)
            throw new IllegalArgumentException("dataset content over 65535 bytes");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(id);
        out.write(content.length >> 8);
        out.write(content.length & 0xFF);
        out.writeBytes(content);
        return out.toByteArray();
    }
}
