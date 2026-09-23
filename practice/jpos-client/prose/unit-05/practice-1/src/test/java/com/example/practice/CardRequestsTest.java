package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class CardRequestsTest {

    // A standard test card number: no bank issues it.
    private static final String PAN = "4111111111111111";
    private static final String AMOUNT = "000000010000";
    private static final String TID = "TERM0001";
    private static final String RRN = "123456789012";

    private final CardRequests requests = new CardRequests();

    private static void common(ISOMsg msg, String processingCode) throws Exception {
        assertEquals("0100", msg.getMTI());
        assertEquals(PAN, msg.getString(2));
        assertEquals(processingCode, msg.getString(3));
        assertEquals(TID, msg.getString(41));
        assertEquals("840", msg.getString(49));
    }

    @Test
    void eachTransactionTypeIsNamedByItsProcessingCode() throws Exception {
        ISOMsg purchase = requests.purchase("visa", PAN, AMOUNT, TID);
        common(purchase, "000000");
        assertEquals(AMOUNT, purchase.getString(4));
        assertFalse(purchase.hasField(37));

        ISOMsg cash = requests.cashAdvance("visa", PAN, AMOUNT, TID);
        common(cash, "010000");
        assertEquals(AMOUNT, cash.getString(4));

        ISOMsg refund = requests.refund("visa", PAN, AMOUNT, TID, RRN);
        common(refund, "200000");
        assertEquals(AMOUNT, refund.getString(4));
        assertEquals(RRN, refund.getString(37));

        ISOMsg balance = requests.balanceInquiry("visa", PAN, TID);
        common(balance, "300000");
        assertFalse(balance.hasField(4));
        assertFalse(balance.hasField(37));
    }

    @Test
    void eachNetworkAddsItsOwnData() throws Exception {
        ISOMsg visa = requests.purchase("visa", PAN, AMOUNT, TID);
        assertEquals("123456", visa.getString(32));
        assertFalse(visa.hasField(61));
        assertFalse(requests.balanceInquiry("visa", PAN, TID).hasField(61));

        ISOMsg mcPurchase = requests.purchase("mastercard", PAN, AMOUNT, TID);
        common(mcPurchase, "000000");
        assertEquals("654321", mcPurchase.getString(32));
        assertEquals("0000000000000001", mcPurchase.getString(61));
        assertEquals("0000000000000002",
                requests.cashAdvance("mastercard", PAN, AMOUNT, TID).getString(61));
        ISOMsg mcRefund = requests.refund("mastercard", PAN, AMOUNT, TID, RRN);
        assertEquals("0000000000000003", mcRefund.getString(61));
        assertEquals(RRN, mcRefund.getString(37));
        ISOMsg mcBalance = requests.balanceInquiry("mastercard", PAN, TID);
        assertEquals("0000000000000004", mcBalance.getString(61));
        assertEquals("654321", mcBalance.getString(32));
        assertFalse(mcBalance.hasField(4));
    }

    @Test
    void theNetworkNameIgnoresCase() throws Exception {
        assertEquals("123456", requests.purchase("VISA", PAN, AMOUNT, TID).getString(32));
        ISOMsg mc = requests.refund("MasterCard", PAN, AMOUNT, TID, RRN);
        assertEquals("654321", mc.getString(32));
        assertEquals("0000000000000003", mc.getString(61));
    }

    @Test
    void anUnsupportedNetworkIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> requests.purchase("amex", PAN, AMOUNT, TID));
        assertThrows(IllegalArgumentException.class,
                () -> requests.balanceInquiry("discover", PAN, TID));
    }
}
