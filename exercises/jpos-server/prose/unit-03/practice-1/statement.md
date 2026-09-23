An `ISOServer` hands every message that arrives on a connection to its request listeners, one
after another, until one of them returns `true`. The listener gets the message and the
`ISOSource` it came from, and it answers by sending through that source. The page's listener
approves everything it receives; you write it so that it can be trusted inside a real server.

Write `ApprovingListener`, an `org.jpos.iso.ISORequestListener`. Its
`process(ISOSource source, ISOMsg request)` sends one reply through `source`: the request's
fields, the response MTI (a `0200` is answered with a `0210`, a `0800` with a `0810`) and
response code `00` in field 39. It returns `true` once the reply has gone.

Why it matters: the `true` or `false` you return decides whether the server offers the
message to the next listener, and the request you were handed may still be read by others
after you.

- The request you were handed is left exactly as it arrived: same MTI, no field 39.
- If the reply cannot be sent (`send` throws), `process` returns `false` and throws
  nothing: the message was not dealt with, and saying otherwise would hide a lost reply.
