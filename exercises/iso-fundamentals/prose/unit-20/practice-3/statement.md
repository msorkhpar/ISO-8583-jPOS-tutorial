An issuer joins a regional network whose acquirers give every transmission its own STAN and its
own field 7, repeats included. What stays the same across every transmission of one transaction,
and in its reversal, is the acquirer (field 32), the terminal (field 41) and the retrieval
reference number (field 37), which the acquirer assigns once per transaction. Two acquirers may
use the same terminal IDs and the same reference numbers.

In its first week on the network the issuer's ledger posted some completions twice, refunded some
reversals twice, and charged a cardholder for a purchase whose reversal had arrived a few seconds
before the purchase itself.

`IssuerLedger` in `src/main/java/com/example/practice/IssuerLedger.java` receives every `02xx` and
`04xx` message from the network; `handle` and `balance` are written. Write `key`, `onFinancial`
and `onReversal`. Each returns the `Result` that says what it did; the comment on each method says
what it is for. About 20 lines in total.
