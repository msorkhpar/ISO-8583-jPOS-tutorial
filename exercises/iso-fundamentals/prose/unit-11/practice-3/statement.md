Last Friday one issuer's host took ten seconds per answer for half an hour. During that half hour
the switch's other issuers were healthy, yet terminals of every bank timed out, and the next day
the operations team had a pile of reversals to explain.

`IssuerLimit` now sits in front of every issuer link, with a limit for each issuer on how many of
its requests may be in flight at once. Finish the four places marked `TODO` in
`src/main/java/com/example/practice/IssuerLimit.java`:

- `tryEnter` and `leave`: the count of each issuer's requests in flight;
- `forward`: it already sends the request to the issuer, and gives the place back when the issuer
  answers;
- `refused`: the switch's own answer to a request it does not send.

When you're done, an issuer at its limit slows nobody else down, the cardholders it can't serve
learn at once that their card was not the problem, and the limits stay exactly as configured
however the issuer links behave. About fifteen lines in total.

The tests keep requests in flight by answering one request only after the next one was sent, so
everything runs on one thread.
