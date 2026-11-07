package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefundAmountsTest {

    private final RefundAmounts portal = new RefundAmounts();

    private static ISOMsg purchase(String amount, String currency) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(41, "29110001");
        m.set(49, currency);
        return m;
    }

    @Test
    void aPoundRefundIsBuiltAndShown() throws Exception {
        ISOMsg refund = portal.refund(purchase("000000006475", "826"), "42.10");
        assertEquals("0200", refund.getMTI());
        assertEquals("200000", refund.getString(3));
        assertEquals("000000004210", refund.getString(4));
        assertEquals("826", refund.getString(49));
        assertEquals("64.75", portal.shown(purchase("000000006475", "826")));
    }

    @Test
    void aPesoRefundIsSentAsTyped() throws Exception {
        ISOMsg refund = portal.refund(purchase("000000038000", "152"), "25000");
        assertEquals("000000025000", refund.getString(4), "a peso has no smaller unit");
    }

    @Test
    void aDinarRefundCarriesItsThreeDecimals() throws Exception {
        ISOMsg refund = portal.refund(purchase("000000009000", "048"), "7.1");
        assertEquals("000000007100", refund.getString(4), "7.1 Bahraini dinars are 7100 fils");
    }

    @Test
    void anAmountFinerThanItsCurrencyIsRefused() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> portal.refund(purchase("000000003000", "410"), "1200.5"), "a won has no smaller unit");
        assertThrows(IllegalArgumentException.class,
                () -> portal.refund(purchase("000000006475", "826"), "0.125"), "a penny is the smallest pound unit");
    }

    @Test
    void noPennyIsLostOnTheWay() throws Exception {
        ISOMsg refund = portal.refund(purchase("000000006475", "826"), "19.99");
        assertEquals("000000001999", refund.getString(4));
    }

    @Test
    void pastPurchasesAreShownInTheirOwnCurrency() throws Exception {
        assertEquals("38000", portal.shown(purchase("000000038000", "152")));
        assertEquals("9.000", portal.shown(purchase("000000009000", "048")));
    }
}
