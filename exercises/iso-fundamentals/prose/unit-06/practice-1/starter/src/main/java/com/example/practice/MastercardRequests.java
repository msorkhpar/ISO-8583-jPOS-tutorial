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
            throw new UnsupportedOperationException("write fromCode");
        }
    }

    private MastercardRequests() {
    }

    /** An 0200 request of the given type. */
    public static ISOMsg request(ProcessingCode type, String pan, String amount, String merchantId)
            throws ISOException {
        throw new UnsupportedOperationException("write request");
    }

    /** An 0200 refund, carrying the original transaction's reference in field 37. */
    public static ISOMsg refund(String pan, String amount, String merchantId, String originalReference)
            throws ISOException {
        throw new UnsupportedOperationException("write refund");
    }

    /** The type of a message, read from its field 3. */
    public static ProcessingCode typeOf(ISOMsg message) {
        throw new UnsupportedOperationException("write typeOf");
    }

    /** Whether a response approves the transaction. */
    public static boolean isApproved(ISOMsg response) {
        throw new UnsupportedOperationException("write isApproved");
    }
}
