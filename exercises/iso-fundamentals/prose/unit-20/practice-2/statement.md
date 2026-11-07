The acquirer is replacing the store-and-forward queue of its network link. The old one caused four
incidents last quarter:

- after a 40-minute outage the link came back, and the queue sent nothing for another 12 minutes;
- on a night when the link went down and up every few minutes, one reversal stayed at the front of
  the queue until morning, and the 300 completions behind it missed the clearing cut-off;
- a voided sale was charged, because its completion reached the issuer after its reversal;
- reversals that the network would no longer accept disappeared, and nobody could say which.

The network's specification for the link: after an advice's first unanswered attempt, wait the
base delay before the next one, and twice as long after each further failure, but never longer
than the cap. An advice gets at most `limit` attempts, and none at or after `window` from its first
attempt. The constructor takes these four values.

`LinkSaf` in `src/main/java/com/example/practice/LinkSaf.java` stores advices and hands its single
sender what to send. `store`, `acknowledged` and the accessors are written. Write `due`, `failed`,
`linkUp` and `waitAfter`; the comment on each says what it is for. About 25 lines in total.
