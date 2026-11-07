Twice this week the acquirer link dropped in the middle of the day. Each time, a handful of
cardholders at other merchants were told "outcome unknown" for purchases the host had approved,
and the reversal process then reversed a purchase the host had never received. Both times the
log showed a warning from the channel adaptor and nothing else. And one purchase, keyed in as
`31.75`, reached the host as an amount nobody had typed.

The client is `PaymentClient` in `src/main/java/com/example/practice/PaymentClient.java`. Its
constructor already gets the packager the link's channel uses. Finish the two places marked
`TODO`:

- `refusal(...)`: the reason a message must not go to the link, or `null`;
- `pay(...)`: what happens to such a message, and the `Outcome` the caller gets for it.

When you're done, a message the link can't carry never reaches it, and the caller's outcome says
what the host knows about the message. About ten lines in total.

The tests run a real QMUX against a host on the far side of its queues. There is no channel
between them, so nothing on the test's side packs the messages: only your code does.
