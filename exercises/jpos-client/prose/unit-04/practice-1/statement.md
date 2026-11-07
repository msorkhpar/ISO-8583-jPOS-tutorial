Your payment API answers each purchase with an HTTP status, and its callers act on it. A 503 makes
the caller retry at once or offer another payment method. A 504 tells it to wait while a reversal
job settles the purchase. Support has two complaints. On busy days some callers get a 500 with a
`NullPointerException` in the log. And during last week's host restart, every caller waited the
full timeout for a 504, and the reversal job then sent hundreds of reversals for purchases the host
had never seen.

Finish the two places marked `TODO` in `src/main/java/com/example/practice/PaymentEndpoint.java`:

- `handle(...)`: when to answer 503;
- `statusOf(...)`: the status once the wait is over, and which purchases go on `unknown`.

The constants at the top of the class are the four statuses. About seven lines in total.

The tests run a real QMUX against a host on the far side of its queues. That host keeps reading
`host-send` even while the QMUX reports the link as down, the way the adaptor would once it
reconnects.
