A server answers most requests by sending back the same message with a few changes: the
response MTI, a response code and an authorization code. The page does that in a
`TransactionParticipant` by cloning the request. The request stays in the `Context` too,
and later participants (and the log) must see it exactly as it arrived.

Write `DataElementHandler`, a jPOS `TransactionParticipant`. Its `prepare` reads the request
`ISOMsg` from the `Context` under `"REQUEST"` and puts under `"RESPONSE"` a message that:

- carries every data element of the request, with the same values;
- has the response MTI (`0200` becomes `0210`, `0100` becomes `0110`), set by jPOS rather than
  worked out by hand;
- adds field 39 = `00` (approved) and field 38 = `123456` (the authorization code).

It then returns `PREPARED | NO_JOIN | READONLY`.

Watch for:

- the request must come out unchanged: no response MTI and no field 38 or 39 on it;
- a message that is already a response (`0210`, say) has no response to take. jPOS refuses it
  with an `ISOException`. Put that exception in the `Context` under `"EXCEPTION"`, put no
  `"RESPONSE"`, and return `ABORTED | NO_JOIN | READONLY`.

In jPOS 2.1.7 `prepare` is declared as `int prepare(long id, Serializable context)`; the
page's `Context` parameter comes from a later version, so cast it yourself.
