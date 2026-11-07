package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class ChargeMapperTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    private final ChargeMapper mapper = new ChargeMapper();

    private static ISOMsg request(String de3, String de4, String de49, String de13, String de12) throws ISOException {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, de3);
        m.set(4, de4);
        m.set(11, "004417");
        m.set(12, de12);
        m.set(13, de13);
        m.set(41, "29110001");
        m.set(49, de49);
        return m;
    }

    @Test
    void aEuroSaleBecomesACharge() throws Exception {
        ChargeMapper.Charge c = mapper.toCharge(request("000000", "000000004999", "978", "0614", "101500"),
                Instant.parse("2031-06-14T08:15:40Z"), BERLIN);
        assertEquals(new ChargeMapper.Charge("sale", new BigDecimal("49.99"), "EUR",
                LocalDateTime.parse("2031-06-14T10:15:00"), "29110001"), c);
    }

    @Test
    void aPoundRefundBecomesARefundCharge() throws Exception {
        ChargeMapper.Charge c = mapper.toCharge(request("200000", "000000000750", "826", "0302", "161245"),
                Instant.parse("2031-03-02T16:13:00Z"), ZoneOffset.UTC);
        assertEquals(new ChargeMapper.Charge("refund", new BigDecimal("7.50"), "GBP",
                LocalDateTime.parse("2031-03-02T16:12:45"), "29110001"), c);
    }

    @Test
    void eachCurrencyPlacesItsOwnDecimalPoint() throws Exception {
        ChargeMapper.Charge pesos = mapper.toCharge(request("000000", "000000015990", "152", "0910", "120000"),
                Instant.parse("2031-09-10T16:00:30Z"), SANTIAGO);
        assertEquals(new BigDecimal("15990"), pesos.amount());
        assertEquals("CLP", pesos.currency());
        ChargeMapper.Charge dinars = mapper.toCharge(request("000000", "000000012345", "414", "0910", "120000"),
                Instant.parse("2031-09-10T12:00:30Z"), ZoneOffset.UTC);
        assertEquals(new BigDecimal("12.345"), dinars.amount());
    }

    @Test
    void aCurrencyOutsideTheContractIsRefused() throws Exception {
        ISOMsg noCurrency = request("000000", "000000001000", "999", "0910", "120000");
        assertThrows(ISOException.class,
                () -> mapper.toCharge(noCurrency, Instant.parse("2031-09-10T12:00:30Z"), ZoneOffset.UTC));
        ISOMsg dollars = request("000000", "000000001000", "840", "0910", "120000");
        assertThrows(ISOException.class,
                () -> mapper.toCharge(dollars, Instant.parse("2031-09-10T12:00:30Z"), ZoneOffset.UTC));
    }

    @Test
    void aPurchaseFromLastYearMappedAfterNewYearKeepsItsYear() throws Exception {
        ChargeMapper.Charge c = mapper.toCharge(request("000000", "000000002000", "978", "1229", "230000"),
                Instant.parse("2032-01-03T07:00:00Z"), ZoneOffset.UTC);
        assertEquals(LocalDateTime.parse("2031-12-29T23:00:00"), c.localTime());
    }

    @Test
    void aTerminalClockAheadAtNewYearIsAlreadyInTheNewYear() throws Exception {
        ChargeMapper.Charge c = mapper.toCharge(request("000000", "000000002000", "978", "0101", "000004"),
                Instant.parse("2031-12-31T23:59:57Z"), ZoneOffset.UTC);
        assertEquals(LocalDateTime.parse("2032-01-01T00:00:04"), c.localTime());
    }

    @Test
    void aRefundThatNamesAnAccountIsStillARefund() throws Exception {
        ChargeMapper.Charge c = mapper.toCharge(request("200010", "000000000750", "826", "0302", "161245"),
                Instant.parse("2031-03-02T16:13:00Z"), ZoneOffset.UTC);
        assertEquals("refund", c.kind());
        ChargeMapper.Charge sale = mapper.toCharge(request("003000", "000000000750", "826", "0302", "161245"),
                Instant.parse("2031-03-02T16:13:00Z"), ZoneOffset.UTC);
        assertEquals("sale", sale.kind());
    }

    @Test
    void aTransactionTheGatewayHasNoKindForIsRefused() throws Exception {
        for (String de3 : new String[] { "010000", "400000", "090000" }) {
            ISOMsg m = request(de3, "000000001000", "978", "0910", "120000");
            assertThrows(ISOException.class,
                    () -> mapper.toCharge(m, Instant.parse("2031-09-10T12:00:30Z"), ZoneOffset.UTC), de3);
        }
    }
}
