`mux.request(request, timeout)` gives back the host's reply, or `null` when no reply came in
time. A reply is not an approval either: the host says yes or no in field 39, the response
code, and only `00` means approved. A client that hands every reply back as if it were a
success lets a declined purchase through; a client that trips over `null` turns a slow host
into a crash. Raising a specific exception for each outcome lets one error handler answer
each one properly.

Write `TransactionService`, built from a jPOS `MUX` and a timeout in milliseconds, with
`ISOMsg send(ISOMsg request)`:

- it asks the MUX for a reply, using that timeout;
- an approved reply (`00`) is returned as it is;
- any other response code raises `TransactionDeclinedException`, a nested unchecked
  exception carrying that response code;
- an `ISOException` from the MUX reaches the caller unchanged.

Edge cases:

- No reply at all raises `java.util.concurrent.TimeoutException`: nobody declined anything.
- A reply with no response code is not an approval. Decline it, with a `null` code.
