The gateway's token vault went live last week, and the incident list is already long:

- the checkout's card-number check refuses nine tokens in ten;
- a token reached a card issuer's authorization system, as if it were a card;
- a customer who shops at a café and at a garage has the same token at both;
- on Tuesday a customer paid with his saved card, and another customer's card was charged;
- the assessor found card numbers in clear among the things the vault keeps.

`tokenize` and `detokenize` in `src/main/java/com/example/practice/TokenVault.java` already
work. Finish the three methods marked `TODO`: `key`, `issue` and `newToken`. The comment on each
says what it is for. Tokens are 16 digits, like the cards they replace. Draw each digit with
`random.nextInt(10)`, one at a time and in order, because the tests script those digits. About
twelve lines in total.

`detokenize` still hands a card number to anyone who asks. The next practice deals with that.
