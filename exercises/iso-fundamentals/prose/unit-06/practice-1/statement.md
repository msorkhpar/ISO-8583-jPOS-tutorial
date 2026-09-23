On a Mastercard link the kind of transaction, purchase, cash advance, void or refund, is not
a field of its own: it is the processing code in field 3. Both sides must agree on the codes,
and a message whose code you do not recognise must never be treated as something it is not.

Write `MastercardRequests`, using its nested enum `ProcessingCode` with the page's codes:
`PURCHASE` `00`, `CASH_ADVANCE` `01`, `VOID` `02`, `REFUND` `20`.

- `ProcessingCode.fromCode(String code)` returns the type a code stands for.
- `request(ProcessingCode type, String pan, String amount, String merchantId)` builds an `0200`
  with the card number in field 2, the type's code in field 3, the amount in field 4 and the
  merchant id in field 42.
- `refund(String pan, String amount, String merchantId, String originalReference)` builds the
  same request for `REFUND`, and also carries the original transaction's reference in field 37.
- `typeOf(ISOMsg message)` reads a message's type back from its field 3.
- `isApproved(ISOMsg response)` says whether the response approves the transaction: its
  field 39 is `00`.

Edge cases to handle:

- A code that is not one of the four is refused with an `IllegalArgumentException`, whether it
  reaches `fromCode` directly or through `typeOf`.
- A response with no field 39 at all is not approved, and `isApproved` answers `false` rather
  than failing.
