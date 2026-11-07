package com.example.practice;

import java.io.ByteArrayOutputStream;
import java.util.List;

/** Builds the chip data (DE 55) of an authorization response, byte by byte. */
public final class IssuerResponse {

    /** An issuer script: template 0x71 or 0x72, an identifier of 4 bytes or null, its commands. */
    public record Script(int template, byte[] scriptId, List<byte[]> commands) {
    }

    private IssuerResponse() {
    }

    /** Tag 91 (when given), then each script's template, in the order given. */
    public static byte[] de55(byte[] issuerAuthenticationData, List<Script> scripts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (issuerAuthenticationData != null) {
            if (issuerAuthenticationData.length < 8 || issuerAuthenticationData.length > 16)
                throw new IllegalArgumentException("tag 91 holds 8 to 16 bytes");
            out.writeBytes(tlv(new byte[] { (byte) 0x91 }, issuerAuthenticationData));
        }
        for (Script s : scripts)
            out.writeBytes(template(s));
        return out.toByteArray();
    }

    /** One element: its tag, its length, its value. */
    static byte[] tlv(byte[] tag, byte[] value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(tag);
        out.writeBytes(length(value.length));
        out.writeBytes(value);
        return out.toByteArray();
    }

    /** The bytes that announce a value of {@code n} bytes. */
    static byte[] length(int n) {
        if (n < 0x80)
            return new byte[] { (byte) n };
        if (n < 0x100)
            return new byte[] { (byte) 0x81, (byte) n };
        return new byte[] { (byte) 0x82, (byte) (n >> 8), (byte) n };
    }

    /** The whole script template: its tag, its length and what it holds. */
    static byte[] template(Script s) {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        content.writeBytes(tlv(new byte[] { (byte) 0x9F, 0x18 }, s.scriptId() == null ? new byte[0] : s.scriptId()));
        for (byte[] command : s.commands())
            content.writeBytes(tlv(new byte[] { (byte) 0x86 }, command));
        return tlv(new byte[] { (byte) s.template() }, content.toByteArray());
    }
}
