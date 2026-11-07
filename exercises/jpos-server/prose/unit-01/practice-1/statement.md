A terminal vendor shipped a software update with a bug: now and then its terminals send a purchase
or an authorization without an amount. The authorizer behind the server can't decide on those, and
today the terminals wait out their 30 seconds, then reverse something the server never processed.
The scheme's rules say such a request gets response code `30` (format error), at once.

`RequiredFields` in `src/main/java/com/example/practice/RequiredFields.java` is the first
participant of the server's chain, ahead of the authorizer and `SendResponse`. Its `prepare`
already works. Finish the two methods marked `TODO`:

- `isComplete(...)`: which requests the authorizer can take;
- `refuse(...)`: how the transaction of any other request ends.

When you're done, a request without an amount gets its `30` from `SendResponse` and never
reaches the authorizer, its terminal recognises the answer as its own, and every other request,
the terminals' echoes included, goes on as before. About ten lines in total.

The tests deploy a real TransactionManager with Q2, in the test's own JVM, and feed it requests
the way `IncomingListener` does.
