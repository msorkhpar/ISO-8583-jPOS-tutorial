In a jPOS switch, each step of handling a transaction is a `TransactionParticipant`. Its
`prepare` reads what it needs from the transaction's `Context`, and either leaves its result there
and says `PREPARED`, or records why it cannot go on and says `ABORTED`. Here you write the step
that approves Visa purchases.

Field 3, the processing code, is six digits, and its **first two** name the transaction:
`00` purchase, `01` cash advance, `02` void, `20` refund. The other four digits say which
accounts are involved and do not change what the transaction is.

Write `VisaPurchaseParticipant`:

- `static String transactionType(String processingCode)` returns `"Purchase"`, `"Cash Advance"`,
  `"Void"` or `"Refund"`, or `null` for a code that is none of them (or is `null`).
- The constructor takes the merchant's card acceptor ID and its name and location.
- `prepare(long id, Serializable context)` takes the `ISOMsg` under `"REQUEST"` in the `Context`.
  When it is an 0100 whose processing code names a purchase, it puts under `"RESPONSE"` an 0110
  carrying everything the request carried, plus field 39 `00`, field 42 set to the acceptor ID
  and field 43 to the name and location, right-padded with spaces to 15 and 40 characters,
  and returns `PREPARED`.
- Anything else (another MTI, another transaction, no processing code) puts
  `"Invalid transaction type"` under `"RESULT"` and returns `ABORTED`.
- The request stays in the context as it arrived: the response is a message of its own.

`commit` and `abort` have nothing to do here.
