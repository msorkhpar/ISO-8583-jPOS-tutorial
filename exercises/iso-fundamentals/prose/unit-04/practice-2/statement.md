Every request on this page has its response: 0100 is answered by 0110, 0200 by 0210, 0400 by
0410, 0800 by 0810. Before you act on a response's field 39, you must be sure it answers the
request you sent; approving a purchase on somebody else's "00" is how money goes missing.

Write `Exchange.outcome(ISOMsg request, ISOMsg response)`. It returns `Outcome.APPROVED` when
the response's field 39 is `00` and `Outcome.DECLINED` for any other response code, but only
once the response is known to answer the request:

- The request's third MTI digit must say it is a request (`0`), and the response's MTI must be
  the request's with that digit changed to response (`1`). Nothing else in the MTI changes.
- The response must carry the same STAN (field 11) as the request.
- The response must carry a response code (field 39).

Anything else is refused with an `IllegalArgumentException`. `Outcome` is the nested enum in the
starter.
