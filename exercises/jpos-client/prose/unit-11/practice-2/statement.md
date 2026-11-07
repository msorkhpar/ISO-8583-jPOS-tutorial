An acquirer runs two links, `acq-mux` and a second one, behind a pool with `check-enabled`, and
one sign-on manager per link. Every Tuesday night operations take `acq-mux` out for maintenance
by stopping its manager, and every Wednesday morning there are complaints:

- a handful of purchases routed to `acq-mux` during the maintenance stop came back declined;
- the host's operators ask why they keep receiving an 0800 with code 002 on connections that
  never signed on, and on links that are down;
- when the manager is deployed again, `acq-mux` sometimes stays out of the pool until the next
  day, and sometimes only when the host did not answer the 002.

The manager's per-link logic is `LinkSession` in
`src/main/java/com/example/practice/LinkSession.java`. `step()` works. Write `signOff()`, which
the manager calls when it is stopped. About five lines.

The tests use a plain `TSpace`, write the adaptor's ready entry themselves, and play the host
through the `Exchanger`.
