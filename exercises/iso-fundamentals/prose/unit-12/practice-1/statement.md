Your switch now takes purchases from a bank's terminals in Germany, the United Kingdom, Switzerland,
Chile and Kuwait, and forwards them to a REST gateway as `POST /charges`. Finance has found charges
that don't match what cardholders signed for, and the gateway's operator has sent back requests it
says should never have reached it.

`ChargeMapper.toCharge` in `src/main/java/com/example/practice/ChargeMapper.java` builds each
`Charge` from a terminal's 1987 request. It is written except for three methods marked `TODO`:

- `kind`: the gateway knows two kinds, `"sale"` and `"refund"`;
- `amount`: the amount as the gateway reads it, a decimal number in the request's currency. The
  currencies in the gateway's contract are in `ACCEPTED`;
- `localTime`: the terminal's local date and time of the purchase, as the gateway's reports and
  date checks will read it.

`toCharge` is called with the moment of mapping and the terminal's time zone, so that the tests can
map requests at any moment of the year. A request the gateway can't take is refused with an
`ISOException` and never sent. About ten lines in total.
