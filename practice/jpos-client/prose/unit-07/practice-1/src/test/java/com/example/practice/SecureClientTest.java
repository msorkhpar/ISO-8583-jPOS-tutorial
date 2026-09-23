package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

class SecureClientTest {

    // A standard test card number: no bank issues it.
    private static final String PAN = "4111111111111111";

    /** A stand-in security module: a readable "encryption" and a digest over every field. */
    private static final class FakeSecurity implements SecureClient.Security {
        @Override
        public String encryptPin(String pin, String pan) {
            return "ENC(" + pin + "," + pan + ")";
        }

        @Override
        public byte[] mac(ISOMsg msg) throws ISOException {
            StringBuilder text = new StringBuilder(msg.getMTI());
            for (int i = 1; i <= 128; i++) {
                if (msg.hasField(i)) {
                    Object value = msg.getValue(i);
                    text.append('|').append(i).append('=').append(value instanceof byte[]
                            ? ISOUtil.hexString((byte[]) value) : String.valueOf(value));
                }
            }
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(text.toString().getBytes(StandardCharsets.UTF_8));
                return Arrays.copyOf(digest, 8);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** A stand-in host: records the request and answers it, signed or spoiled as asked. */
    private static final class FakeHost implements SecureClient.Link {
        enum Answer { SIGNED, TAMPERED, UNSIGNED }

        private final FakeSecurity security = new FakeSecurity();
        private final Answer answer;
        ISOMsg received;

        FakeHost(Answer answer) {
            this.answer = answer;
        }

        @Override
        public ISOMsg exchange(ISOMsg request) throws ISOException {
            received = (ISOMsg) request.clone();
            ISOMsg response = new ISOMsg();
            response.setMTI("0210");
            response.set(2, request.getString(2));
            response.set(4, request.getString(4));
            response.set(11, request.getString(11));
            response.set(39, "00");
            if (answer != Answer.UNSIGNED) {
                response.set(64, security.mac(response));
            }
            if (answer == Answer.TAMPERED) {
                response.set(4, "000000999999");
            }
            return response;
        }
    }

    private static ISOMsg purchase() throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        msg.set(2, PAN);
        msg.set(3, "000000");
        msg.set(4, "000000012345");
        msg.set(11, "000042");
        msg.set(52, "1234");
        return msg;
    }

    @Test
    void sendsAnEncryptedPinAndAMacOverWhatItSends() throws Exception {
        FakeSecurity security = new FakeSecurity();
        FakeHost host = new FakeHost(FakeHost.Answer.SIGNED);
        ISOMsg response = new SecureClient(security, host).send(purchase());

        assertNotNull(host.received, "the request was exchanged");
        assertEquals("ENC(1234," + PAN + ")", host.received.getString(52));
        byte[] sentMac = host.received.getBytes(64);
        assertNotNull(sentMac, "the request carries a MAC in field 64");
        ISOMsg unsigned = (ISOMsg) host.received.clone();
        unsigned.unset(64);
        assertArrayEquals(security.mac(unsigned), sentMac);

        assertNotNull(response);
        assertEquals("0210", response.getMTI());
        assertEquals("00", response.getString(39));
    }

    @Test
    void aTamperedResponseIsRefused() throws Exception {
        SecureClient client = new SecureClient(new FakeSecurity(),
                new FakeHost(FakeHost.Answer.TAMPERED));
        assertThrows(SecurityException.class, () -> client.send(purchase()));
    }

    @Test
    void aResponseWithoutAMacIsRefused() throws Exception {
        SecureClient client = new SecureClient(new FakeSecurity(),
                new FakeHost(FakeHost.Answer.UNSIGNED));
        assertThrows(SecurityException.class, () -> client.send(purchase()));
    }
}
