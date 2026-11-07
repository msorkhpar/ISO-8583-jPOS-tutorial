package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoardingTest {

    private static Properties merchant(String name, String city, String country, String... acquirers) {
        Properties p = new Properties();
        p.setProperty("terminal", "29110014");
        p.setProperty("merchant", "000000007654321");
        p.setProperty("mcc", "5411");
        p.setProperty("currency", "978");
        p.setProperty("name", name);
        p.setProperty("city", city);
        p.setProperty("country", country);
        for (int i = 0; i < acquirers.length; i += 2)
            p.setProperty("acquirer." + acquirers[i], acquirers[i + 1]);
        return p;
    }

    private static Properties bothNetworks(String name, String city, String country) {
        return merchant(name, city, country, "visa", "438217", "mastercard", "527409");
    }

    /** What a host reads from field 43, after the message went over the wire. */
    private static String[] hostReads43(ISOMsg m) throws Exception {
        GenericPackager p = new GenericPackager("jar:packager/iso87ascii.xml");
        ISOMsg out = (ISOMsg) m.clone();
        out.setPackager(p);
        ISOMsg in = new ISOMsg();
        in.setPackager(p);
        in.unpack(out.pack());
        String f = in.getString(43);
        assertEquals(40, f.length(), "field 43 is always 40 characters");
        return new String[] { f.substring(0, 25).trim(), f.substring(25, 38).trim(), f.substring(38, 40).trim() };
    }

    @Test
    void aVisaPurchaseCarriesTheMerchantsBoarding() throws Exception {
        Boarding b = new Boarding(bothNetworks("RIVERSIDE BAKERY AND CAFE", "SAN FRANCISCO", "US"));
        ISOMsg m = b.purchase("visa", "4111111111111111", 3799);
        assertEquals("438217", m.getString(32));
        assertEquals("29110014", m.getString(41));
        assertEquals("000000007654321", m.getString(42));
        assertEquals("000000003799", m.getString(4));
        String[] f43 = hostReads43(m);
        assertEquals("RIVERSIDE BAKERY AND CAFE", f43[0]);
        assertEquals("SAN FRANCISCO", f43[1]);
        assertEquals("US", f43[2]);
    }

    @Test
    void aMastercardPurchaseCarriesItsOwnAcquirerId() throws Exception {
        Boarding b = new Boarding(bothNetworks("RIVERSIDE BAKERY AND CAFE", "SAN FRANCISCO", "US"));
        assertEquals("527409", b.purchase("Mastercard", "5555555555554444", 120).getString(32));
        assertEquals("438217", b.purchase("VISA", "4111111111111111", 120).getString(32));
    }

    @Test
    void aNetworkTheMerchantIsNotBoardedForIsRefused() {
        Boarding b = new Boarding(merchant("RIVERSIDE BAKERY AND CAFE", "SAN FRANCISCO", "US", "visa", "438217"));
        assertThrows(IllegalArgumentException.class, () -> b.purchase("mastercard", "5555555555554444", 2000),
                "a purchase without an acquirer ID must not be built");
    }

    @Test
    void anUnknownNetworkIsNotSentAsAnother() {
        Boarding b = new Boarding(bothNetworks("RIVERSIDE BAKERY AND CAFE", "SAN FRANCISCO", "US"));
        assertThrows(IllegalArgumentException.class, () -> b.purchase("discover", "4111111111111111", 2000),
                "a network nobody boarded must not borrow another network's acquirer ID");
    }

    @Test
    void aShortNameLeavesTheCityWhereTheHostLooks() throws Exception {
        Boarding b = new Boarding(bothNetworks("CORNER DELI", "OAKDALE", "US"));
        String[] f43 = hostReads43(b.purchase("visa", "4111111111111111", 650));
        assertEquals("CORNER DELI", f43[0]);
        assertEquals("OAKDALE", f43[1]);
        assertEquals("US", f43[2]);
    }

    @Test
    void aLongNameStillFitsTheField() throws Exception {
        Boarding b = new Boarding(bothNetworks("NORTHSIDE HARDWARE AND GARDEN SUPPLY", "OAKDALE", "US"));
        String[] f43 = hostReads43(b.purchase("visa", "4111111111111111", 650));
        assertEquals("NORTHSIDE HARDWARE AND GA", f43[0]);
        assertEquals("OAKDALE", f43[1]);
        assertEquals("US", f43[2]);
    }

    @Test
    void aLongCityStillLeavesTheCountryInPlace() throws Exception {
        Boarding b = new Boarding(bothNetworks("CORNER DELI", "NORTH HOLLYWOOD", "US"));
        String[] f43 = hostReads43(b.purchase("visa", "4111111111111111", 650));
        assertEquals("NORTH HOLLYWO", f43[1]);
        assertEquals("US", f43[2]);
    }
}
