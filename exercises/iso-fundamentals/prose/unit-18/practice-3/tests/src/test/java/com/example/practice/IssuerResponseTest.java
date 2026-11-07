package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.practice.IssuerResponse.Script;
import java.util.List;
import org.jpos.iso.ISOUtil;
import org.jpos.tlv.TLVList;
import org.junit.jupiter.api.Test;

class IssuerResponseTest {

    private static byte[] h(String hex) {
        return ISOUtil.hex2byte(hex);
    }

    private static byte[] bytes(int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++)
            b[i] = (byte) (i + 7);
        return b;
    }

    // Method 1: an 8-byte ARPC and the response code "05" in ASCII
    private static final String T91 = "0BADF00DCAFE1234" + "3035";
    private static final String PIN_UNBLOCK = "8424000008" + "A1A2A3A4A5A6A7A8";
    private static final String APP_BLOCK = "841E000008" + "B1B2B3B4B5B6B7B8";

    @Test
    void tag91ComesFirstThenEachScriptInOrder() {
        String hex = ISOUtil.hexString(IssuerResponse.de55(h(T91), List.of(
                new Script(0x71, h("0000A001"), List.of(h(PIN_UNBLOCK))),
                new Script(0x72, h("0000A002"), List.of(h(APP_BLOCK))))));
        assertTrue(hex.startsWith("910A" + T91), hex);
        int first = hex.indexOf(PIN_UNBLOCK), second = hex.indexOf(APP_BLOCK);
        assertTrue(first > 0 && second > first, hex);
        assertTrue(hex.indexOf("0000A001") < first && hex.indexOf("0000A002") > first, hex);
        assertEquals(0, IssuerResponse.de55(null, List.of()).length);
    }

    @Test
    void aTemplatesLengthCountsItsEncodedElements() {
        String hex = ISOUtil.hexString(IssuerResponse.de55(null, List.of(
                new Script(0x71, h("0000A001"), List.of(h(PIN_UNBLOCK), h("8418000000"))))));
        assertEquals("711D" + "9F18040000A001" + "860D" + PIN_UNBLOCK + "8605" + "8418000000", hex);
        TLVList outer = new TLVList();
        outer.unpack(h(hex));
        TLVList inner = new TLVList();
        inner.unpack(outer.getValue(0x71));
        assertEquals(3, inner.getTags().size());
    }

    @Test
    void longValuesGetTheLongLengthForm() {
        byte[] de55 = IssuerResponse.de55(null, List.of(new Script(0x72, h("0000A003"), List.of(bytes(140)))));
        // 86 81 8C + 140 = 143, plus 7 for 9F18: 150 = 0x96
        assertEquals("728196" + "9F18040000A003" + "86818C", ISOUtil.hexString(de55).substring(0, 26));
        assertEquals(3 + 150, de55.length);

        de55 = IssuerResponse.de55(null, List.of(new Script(0x71, null, List.of(bytes(250), bytes(30)))));
        // 3 + 250 + 2 + 30 = 285 = 0x011D
        assertEquals("7182011D" + "8681FA", ISOUtil.hexString(de55).substring(0, 14));
        TLVList outer = new TLVList();
        outer.unpack(de55);
        TLVList inner = new TLVList();
        inner.unpack(outer.getValue(0x71));
        assertEquals(2, inner.getTags().size());
        assertEquals(ISOUtil.hexString(bytes(30)), ISOUtil.hexString(inner.getTags().get(1).getValue()));
    }

    @Test
    void aScriptWithoutAnIdentifierHasNoIdentifierElement() {
        String hex = ISOUtil.hexString(IssuerResponse.de55(h(T91), List.of(
                new Script(0x72, null, List.of(h(APP_BLOCK))))));
        assertEquals("910A" + T91 + "720F" + "860D" + APP_BLOCK, hex);
    }
}
