This practice continues the gift-card server: `src/main/java/com/example/practice/GiftCards.java`
starts from the previous practice's solution.

Support settles disputes from the `redemption_log` table, and last month it couldn't: a cardholder
complained about a redemption that was declined, and the table had no row for it. Write `Record`,
marked `TODO`. It runs after `SendResponse`, and every redemption, approved or declined, gets one
row through the given `insertRow`, with `COMMITTED` or `ABORTED` as its outcome.

Support must be able to find every row for a card number it is given, and a stolen copy of the
table must not give card numbers away. The secret key in the field `key` is there for that, and
`card_lookup` holds 64 lowercase hex characters. About 20 lines in total.

The tests run the whole chain in a real TransactionManager, with a database that allows three open
connections at a time.
