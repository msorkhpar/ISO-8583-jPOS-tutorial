This builds on the previous practice: `PaymentEndpoint.java` already contains its solution.

The payment API is moving to a non-blocking web framework. A request handler there must return at
once and hand back a `CompletableFuture`, and the framework sends the HTTP response when the future
completes. A future that never completes is a caller that hangs until its own client gives up.

Write `handleAsync(...)` in `src/main/java/com/example/practice/PaymentEndpoint.java`. Its callers
expect the same statuses as from `handle(...)`, and the same entries on `unknown`. About ten lines.

The tests use the same QMUX and host as before. In some of them the host holds a purchase and
answers only when the test lets it.
