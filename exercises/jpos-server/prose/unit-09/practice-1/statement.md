The server now takes gift cards. A redemption is a 0200 with the gift card's number in field 2 and
the amount in field 4, and each card's balance is a row in the `gift_cards` table. The chain is
`Open`, `Redeem`, a fraud check, `Close` and `SendResponse`, all in
`src/main/java/com/example/practice/GiftCards.java`.

`Open`, `Close` and the `debit`, `balance` and `reply` helpers are given. Write `Redeem.prepare`,
marked `TODO`. The cashier needs `00` when the card paid, `51` when its balance was too small, and
`96` when the database failed. Support has two rules from past incidents: a card whose redemption
was declined anywhere in the chain keeps its whole balance, and two lanes redeeming the same card at
the same moment never spend more than the card holds. About 15 lines.

The tests deploy a real TransactionManager with Q2 in the test's JVM, in front of a small database
that has transactions and row locks.
