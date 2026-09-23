Your switch speaks ISO-8583; the payment gateway behind it speaks a REST API with its own
names and formats. The bridge between them has two halves: turn the request into the
gateway's request, and turn the gateway's reply back into the ISO-8583 response your
acquirer is waiting for.

Write `GatewayBridge.toGateway(ISOMsg request, LocalDate today)`, returning a
`PaymentRequest` with:

- `amount`: field 4, an amount in minor units, with its decimal point (`000000100000`
  becomes `1000.00`);
- `cardNumber`: field 2, as it is;
- `expirationDate`: field 14, `YYMM`, as `MM/YY` (`2405` becomes `05/24`);
- `transactionType`: from the processing code in field 3, `000000` is `PURCHASE` and
  `200000` is `REFUND`;
- `transactionDate`: field 13, `MMDD`, as `YYYY-MM-DD`, in the year of `today`.

Then write `GatewayBridge.toIso(ISOMsg request, GatewayResponse reply)`. It answers the
request: a copy of it with the response MTI, and

- when the reply's status is `SUCCESS`: field 39 `00` and the reply's transaction id in
  field 38;
- otherwise: field 39 `05` and the reply's error message in field 63, with no field 38.

Edge cases:

- Field 13 carries no year. A month and day still ahead of `today` cannot have happened
  this year, so it belongs to the year before.
- A processing code that is neither `000000` nor `200000` is refused with an
  `IllegalArgumentException`: sending the gateway a guessed transaction type moves money
  the wrong way.
