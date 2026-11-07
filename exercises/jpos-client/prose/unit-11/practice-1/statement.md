A small merchant site runs the checkout client of unit c4 on a single link: one QMUX, no pool.
The host restarted at 03:10, the adaptor reconnected a few seconds later, and until the sign-on
manager got through, every purchase went to the host and came back declined. The merchant would
rather tell the cashier "host unavailable" straight away and keep the card for a second try.

`CheckoutClient.authorize` in `src/main/java/com/example/practice/CheckoutClient.java` already
asks `signedOn()` before it sends anything. Write `signedOn()`. The client gets the same Space as
the sign-on manager, and the constructor has already worked out the two entry names you need.
Two or three lines.

The tests run a real QMUX against a host that approves everything, and they play the adaptor and
the sign-on manager themselves by writing to the Space.
