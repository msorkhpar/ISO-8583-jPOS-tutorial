package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.ISO87APackager;
import org.junit.jupiter.api.Test;

class MessageTemplatesTest {

    private static ISOMsg template(String mti, String processingCode) throws Exception {
        ISOMsg template = new ISOMsg();
        template.setMTI(mti);
        template.set(3, processingCode);
        template.set(25, "00");
        template.set(41, "12345678");
        return template;
    }

    private static MessageTemplates purchaseAndBalance() throws Exception {
        MessageTemplates templates = new MessageTemplates(new ISO87APackager());
        templates.define("purchase", template("0200", "000000"));
        templates.define("balance", template("0100", "310000"));
        return templates;
    }

    @Test
    void buildsEachMessageFromItsTemplateAndSurvivesAPackRoundTrip() throws Exception {
        MessageTemplates templates = purchaseAndBalance();

        ISOMsg purchase = templates.newMessage("purchase");
        purchase.set(2, "4111111111111111");
        purchase.set(4, "000000010000");
        purchase.set(11, "000123");
        assertEquals("0200", purchase.getMTI());
        assertEquals("000000", purchase.getString(3));
        assertEquals("12345678", purchase.getString(41));

        ISOMsg back = templates.unpack(templates.pack(purchase));
        assertEquals("0200", back.getMTI());
        for (int field : new int[] {2, 3, 4, 11, 25, 41}) {
            assertEquals(purchase.getString(field), back.getString(field), "field " + field);
        }
        assertArrayEquals(templates.pack(purchase), templates.pack(back));

        ISOMsg balance = templates.newMessage("balance");
        assertEquals("0100", balance.getMTI());
        assertEquals("310000", balance.getString(3));
    }

    @Test
    void eachMessageStartsFromAnUntouchedTemplate() throws Exception {
        MessageTemplates templates = purchaseAndBalance();

        ISOMsg first = templates.newMessage("purchase");
        first.set(2, "4111111111111111");
        first.set(41, "99999999");

        ISOMsg second = templates.newMessage("purchase");
        assertFalse(second.hasField(2), "the first message's card leaked into the second");
        assertEquals("12345678", second.getString(41));
    }

    @Test
    void anUnknownTemplateIsRefused() throws Exception {
        MessageTemplates templates = purchaseAndBalance();
        assertThrows(IllegalArgumentException.class, () -> templates.newMessage("refund"));
    }
}
