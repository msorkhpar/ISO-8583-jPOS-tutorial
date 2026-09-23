The acquirer does not talk to the issuer directly. It hands the authorization request to a
jPOS `MUX`, which sends it on and matches the answer that comes back, and it waits only so
long. What the acquirer does with that answer, or with its absence, decides what the
cardholder is told at the terminal.

Write `Authorizer.authorize(MUX mux, ISOMsg request)`. It sends the request with
`mux.request(request, Authorizer.TIMEOUT_MILLIS)` (30 seconds) and returns a `Decision`:

- `approved`: whether the issuer approved, which it does only with `00` in field 39;
- `responseCode`: the code the issuer returned in field 39, so a decline such as `05` or
  `51` can be passed on.

Edge case to handle:

- When the MUX gives back no response (it returns `null`, which is how a timeout arrives),
  the issuer has **not** declined: nobody knows what it decided. Throw an `IOException`
  saying there was no response; never turn it into a declined `Decision`.
