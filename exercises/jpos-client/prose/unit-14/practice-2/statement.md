A chain of stores pays through a `MUXPool` over two links to its acquirer. Since the second link was
added, two complaints have come in. On Tuesday a link dropped at noon, and a few cardholders were
charged twice for one purchase, although the checkout showed a single approval. And during the
nightly maintenance, when both links are down, every checkout hangs for a second before failing,
and the reversal queue fills with purchases the acquirer has never seen.

`Checkout.pay` in `src/main/java/com/example/practice/Checkout.java` already handles purchases the
host answers. Finish the two methods marked `TODO`:

- `canSend()`: whether to send at all;
- `onNoAnswer(...)`: what a purchase with no answer becomes.

Purchases that must be reversed go onto the `TO_REVERSE` queue of the space (`sp.out`), where the
reversal sender takes them. About four lines in total.

The tests run a real round-robin `MUXPool` over two real QMUXes, with a test host behind each link.
