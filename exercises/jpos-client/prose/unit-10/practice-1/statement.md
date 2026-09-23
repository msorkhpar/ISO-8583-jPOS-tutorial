A client test that needs a real host is slow, depends on a network, and cannot make the host
decline on demand. Everything the client does passes through its `MUX`, so the client can be
tested against a stand-in that answers in the same process, the way the host would, and
whose answers the test controls.

Write `SimulatedHost`, a jPOS `MUX` built from an amount limit in minor units (cents). Its
`request(ISOMsg request, long timeout)` answers at once with a reply that:

- has the response MTI of the request (`0100` is answered with `0110`, `0800` with `0810`);
- echoes every field the request carried;
- sets field 39 to `51` (insufficient funds) when the amount in field 4 is over the limit,
  and to `00` otherwise.

The other `MUX` methods: `isConnected()` is always true, `send` accepts a message and answers
nothing, and the asynchronous `request` hands the same reply straight to the listener.

Edge cases:

- The reply is a new message. The client still holds its request, and a test may still
  check it, so answering must not change it.
- A request that carries no amount, such as a network echo test, is approved.
