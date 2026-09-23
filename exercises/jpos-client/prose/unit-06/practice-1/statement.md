The page turns a request into a response by changing it in place: a new MTI, a response code,
the EMV data removed. That is a hazard in a client or server that still holds the request, to
log it, match it or retry it, because after the call the request is no longer the request.

Write `ResponseMaker` with `respond(ISOMsg request, String responseCode)`. It returns a new
`ISOMsg` that:

- has the response MTI for the request's MTI: the third digit goes up by one, so a `0200` is
  answered with a `0210`;
- carries every field of the request, with the same values;
- carries `responseCode` in field 39;
- does not carry field 55 (the chip's EMV data), whether or not the request had it.

Mind these:

- Do not assume every request is a `0200`. An authorization `0100` is answered with a `0110`.
- The request passed in must come out of the call exactly as it went in: same MTI, same fields,
  its field 55 still there, and no field 39 added to it.
