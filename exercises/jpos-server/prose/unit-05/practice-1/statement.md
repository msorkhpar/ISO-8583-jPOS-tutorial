Visa and Mastercard ask for different data elements in an authorization, so a server first
works out which network a card belongs to and then checks the request against that network's
rules. The page does it with three participants in a row. Here you do both steps in one
jPOS `TransactionParticipant`, in the same order.

Write `CardNetworkCheck`. Its `prepare` reads the request `ISOMsg` from the `Context` under
`"REQUEST"` and:

- puts `"CARD_TYPE"`: `"VISA"` when the card number (field 2) starts with `4`,
  `"MASTERCARD"` when it starts with `51` to `55`, and `"UNKNOWN"` otherwise;
- for a Visa card, puts `"VISA_TRANSACTION_VALID"`: `true` when fields 42, 43 and 48 are all
  present, else `false` together with a `"VISA_ERROR"` saying what is wrong;
- for a Mastercard card, does the same with fields 48, 61 and 63, under
  `"MASTERCARD_TRANSACTION_VALID"` and `"MASTERCARD_ERROR"`;
- always returns `PREPARED | NO_JOIN | READONLY`. An invalid request is a verdict for the
  participants after this one to act on, not a reason to throw.

Watch for:

- a card starting `56` (or `50`) is not Mastercard. An unknown card gets no network verdict at all;
- a request with no card number is `"UNKNOWN"`, and the participant must not crash on it.

In jPOS 2.1.7 `prepare` is declared as `int prepare(long id, Serializable context)`; the
page's `Context` parameter comes from a later version, so cast it yourself.
