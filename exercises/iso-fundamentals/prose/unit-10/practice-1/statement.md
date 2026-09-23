You cannot certify, or even test, against a live card network every time you change your
code. A host simulator stands in for the network: it takes each request your system sends
and answers it the way the network would, and designated test card numbers let a test pick
the outcome it wants.

Write the answering half of such a simulator (no sockets: the part a request listener would
call). `new HostSimulator(approvalCode)` approves every card unless told otherwise, and
`declineCard(pan, responseCode)` sets one test card up to be declined with that code.

`respond(ISOMsg request)` returns the response:

- it carries every field of the request, with the MTI turned into the matching response MTI
  (a `0100` is answered with a `0110`, a `0200` with a `0210`);
- an approved request gets response code `00` in field 39 and the approval code in field 38;
- a request for a card set up to be declined gets that card's response code in field 39 and
  no field 38, because nothing was approved.

Edge cases:

- The request is not changed. Your caller still holds it, for example to match the answer
  to what it sent.
- A message that is already a response (`0110`, `0210`, ...) cannot be answered; refuse it
  with an `ISOException`.
