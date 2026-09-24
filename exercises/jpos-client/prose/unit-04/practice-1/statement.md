A request through a jPOS `MUX` either waits for its answer or hands it to a listener later. In
both forms the answer may never come: the blocking `request(msg, timeout)` then returns `null`,
and the listener form calls the listener's `expired`. Code that
forgets this throws a `NullPointerException` on a busy day, or leaves a caller waiting on a
result that will never arrive.

In jPOS 2.1.7 the listener is `ISOResponseListener`, with `responseReceived(ISOMsg response,
Object handBack)` and a default `expired(Object handBack)` that calls `responseReceived` with a
`null` response; the asynchronous request is `request(msg, timeout, listener, handBack)`.

Write `AuthorizationSender`, built from a `MUX` and a timeout in milliseconds, and the nested
enum `Outcome { APPROVED, DECLINED, NO_ANSWER }`.

- `authorize(request)` sends the request with that timeout and waits. A response whose field 39
  is `00` is `APPROVED`; any other response is `DECLINED`.
- `authorizeAsync(request)` sends the request with the same timeout and returns a
  `CompletableFuture<Outcome>` at once; the response, when it arrives, completes it the same way.

Edge cases:

- No response in time is `NO_ANSWER`, not an exception.
- An asynchronous request that expires completes its future with `NO_ANSWER`.
