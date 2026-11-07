Your issuer's fraud team found the same chip authorization approved twice, an hour apart, at two
different merchants: identical DE 55, identical cryptogram, and the HSM verified both. They want the
host to keep its own record of chip counters, and they have already had one complaint from a
cardholder whose new card was declined for days after it arrived.

`AtcGuard` in `src/main/java/com/example/practice/AtcGuard.java` is called once per chip
authorization, after the HSM has checked the cryptogram. Finish the three methods marked `TODO`:

- `card`: which card the counter belongs to;
- `atc`: the counter the card sent in DE 55;
- `decide`: the verdict, keeping the record up to date.

The `Verdict` values say what each answer means. About ten lines in total.
