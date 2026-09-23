package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Mastercard financial requests, typed by their processing code in field 3. */
public final class MastercardRequests {

    /** The transaction types and the processing codes that carry them. */
    public enum ProcessingCode {
        PURCHASE("00"),
        CASH_ADVANCE("01"),
        VOID("02"),
        REFUND("20");

        private final String code;

        ProcessingCode(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        /** The type a processing code stands for. */
        public static ProcessingCode fromCode(String code) {
            for (ProcessingCode type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }

    private MastercardRequests() {
    }

    /** An 0200 request of the given type. */
    public static ISOMsg request(ProcessingCode type, String pan, String amount, String merchantId)
            throws ISOException {
        ISOMsg message = new ISOMsg();
        message.setMTI("0200");
        message.set(2, pan);
        message.set(3, type.code());
        message.set(4, amount);
        message.set(42, merchantId);
        return message;
    }

    /** An 0200 refund, carrying the original transaction's reference in field 37. */
    public static ISOMsg refund(String pan, String amount, String merchantId, String originalReference)
            throws ISOException {
        ISOMsg message = request(ProcessingCode.REFUND, pan, amount, merchantId);
        message.set(37, originalReference);
        return message;
    }

    /** The type of a message, read from its field 3. */
    public static ProcessingCode typeOf(ISOMsg message) {
        return ProcessingCode.fromCode(message.getString(3));
    }

    /** Whether a response approves the transaction. */
    public static boolean isApproved(ISOMsg response) {
        return "00".equals(response.getString(39));
    }
}
