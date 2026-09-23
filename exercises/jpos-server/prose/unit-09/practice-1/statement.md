A server keeps a record of every transaction it handles, and the transaction manager is where
that record is made: one participant turns the transaction's `Context` into a record and hands
it to a service. It saves in `prepare` and has nothing to do afterwards, so it tells the
manager to leave it out of the commit and abort phases altogether.

Write `TransactionStorageParticipant`, a jPOS `TransactionParticipant` built from a
`TransactionStore` (the service it saves through; it is declared as a nested interface, next
to the nested `StoredTransaction` record). In `prepare`, read the context entries
`transactionId`, `amount`, `currency`, `cardNumber`, `transactionType` and `status` (all
strings), build one `StoredTransaction` with the amount as a `BigDecimal`, save it, and return
`PREPARED | NO_JOIN | READONLY`.

- The amount is kept exactly as written: `"100.50"` is stored as `100.50`, not rounded.
- A context whose amount is missing or is not a number (`"12,50"`, say) must not throw out of
  `prepare`: a participant that throws takes the transaction down with an error nobody
  handles. Return `ABORTED` instead, and store nothing.
