package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

class ChipDataTest {

    private static ISOMsg withField55(String hex) throws Exception {
        ISOMsg message = new ISOMsg();
        message.setMTI("0100");
        message.set(2, "4761739001010010");
        if (hex != null) {
            message.set(55, ISOUtil.hex2byte(hex));
        }
        return message;
    }

    @Test
    void readsEveryTagWithItsValueInOrder() throws Exception {
        // 82 (AIP) 2 bytes, 95 (TVR) 5 bytes, 84 (application id) 7 bytes.
        Map<String, String> tags = ChipData.tags(withField55("82021980" + "95050000048000" + "8407A0000000031010"));
        assertEquals(List.of("82", "95", "84"), new ArrayList<>(tags.keySet()));
        assertEquals("1980", tags.get("82"));
        assertEquals("0000048000", tags.get("95"));
        assertEquals("A0000000031010", tags.get("84"));

        assertEquals(Map.of(), ChipData.tags(withField55(null)));
    }

    @Test
    void readsTwoByteTags() throws Exception {
        // 82, then 9F26 (cryptogram) 8 bytes, 5F2A (currency) 2 bytes, 95.
        Map<String, String> tags = ChipData.tags(withField55(
            "82021980" + "9F2608A1B2C3D4E5F60718" + "5F2A020840" + "95050000048000"));
        assertEquals(List.of("82", "9F26", "5F2A", "95"), new ArrayList<>(tags.keySet()));
        assertEquals("A1B2C3D4E5F60718", tags.get("9F26"));
        assertEquals("0840", tags.get("5F2A"));
        assertEquals("0000048000", tags.get("95"));
    }

    @Test
    void refusesAValueThatRunsPastTheEnd() throws Exception {
        // 95 claims five bytes but only three follow.
        ISOMsg damaged = withField55("82021980" + "9505000004");
        assertThrows(ISOException.class, () -> ChipData.tags(damaged));
    }
}
