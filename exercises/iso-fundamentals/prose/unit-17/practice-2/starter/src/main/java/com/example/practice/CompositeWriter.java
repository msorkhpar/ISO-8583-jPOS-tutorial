package com.example.practice;

import java.io.ByteArrayOutputStream;
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
        // TODO: Encode each element so that a receiver can find its tag, its value and where the
        //       next element starts. Values can be anything from empty to tens of kilobytes.
        throw new IllegalStateException("TODO: content");
    }

    /** One TLV dataset as it goes into the field. */
    static byte[] envelope(int id, byte[] content) {
        // TODO: Put the dataset into its envelope, so that a receiver walking the field finds it
        //       and steps over it. Refuse what a receiver could not read back as this TLV dataset.
        throw new IllegalStateException("TODO: envelope");
    }
}
