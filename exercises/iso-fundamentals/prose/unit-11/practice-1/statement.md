The acquirer's merchant portal now serves merchants outside the euro and dollar countries, and the
complaints have started. A restaurant in Santiago refunded a dinner and the cardholder got a
hundred times too much back. A shop in Manama says its refunds come out wrong, and a Seoul merchant
says the portal shows every purchase with two decimals the won doesn't have.

Merchants type the amount to refund as they would write it on a receipt (`42.10`, `25000`), and
the portal shows the amount of each past purchase. Finish the two methods marked `TODO` in
`src/main/java/com/example/practice/RefundAmounts.java`:

- `field4`: field 4 of the refund, for the amount the merchant typed in the purchase's currency.
  An amount the currency can't carry exactly is refused with an `IllegalArgumentException`;
- `shown`: the amount of a past purchase as the merchant would write it, such as `42.10`.

When you're done, the issuer reads the amount the merchant typed, in every currency. About five
lines in total.

The tests use other currencies and amounts than the lesson.
