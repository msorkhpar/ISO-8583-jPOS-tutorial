The page writes a Visa service and a Mastercard service, eight methods that are almost the same
code. Read them side by side and the differences are small: the processing code in field 3 says
which transaction it is, and each network adds a little data of its own. When those rules live
in one place, a new transaction type or a changed acquirer ID is one edit, not eight.

Write `CardRequests` with four methods, each taking the card network's name first:
`purchase(network, pan, amount, terminalId)`, `cashAdvance(network, pan, amount, terminalId)`,
`refund(network, pan, amount, terminalId, originalRrn)` and
`balanceInquiry(network, pan, terminalId)`. Each returns a new `ISOMsg` with:

- MTI `0100`, the card number in field 2, the terminal ID in field 41 and currency `840` in
  field 49;
- the processing code in field 3: `000000` for a purchase, `010000` for a cash advance,
  `200000` for a refund and `300000` for a balance inquiry;
- the amount in field 4, except on a balance inquiry, which has no amount;
- on a refund, the original transaction's retrieval reference number in field 37;
- the acquiring institution in field 32: `123456` for Visa, `654321` for Mastercard;
- for Mastercard only, point-of-service data in field 61: `0000000000000001` for a purchase,
  `...02` for a cash advance, `...03` for a refund and `...04` for a balance inquiry (all
  sixteen digits long). A Visa request has no field 61.

Leave out the page's transmission time (field 7) and trace number (field 11); the sender fills
those in when the request goes out.

Mind these:

- The network name is `visa` or `mastercard`, in any case (`VISA`, `MasterCard`).
- Any other network (say `amex`) is refused with an `IllegalArgumentException`. Sending it as a
  Visa request would put the wrong acquirer on a real transaction.
