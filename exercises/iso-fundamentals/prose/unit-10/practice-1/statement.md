Your team's certification harness builds each request of the network's script and sends it to the
network's test host. Two things went wrong last month. A tester checked the link with a card from
their own wallet, and the security team had to treat the build logs, the dumps and a ticket as card
data. Another case, keyed by hand, came back with `14` instead of the answer it was written to
test, and a session slot went with it.

The network has issued test card ranges for this certification, each given as the leading digits
its cards share. Every request now passes through `TestCardGuard.check` before it is sent. Finish
the three methods marked `TODO` in `src/main/java/com/example/practice/TestCardGuard.java`:

- `panOf`: the card number a request carries;
- `isTestCard`: whether the harness may send it;
- `refusal`: the text of the refusal, which goes to the build log.

When you're done, only cards the network issued for this certification reach the test host, keyed
or swiped, and a refusal tells the tester which card it stopped. About fifteen lines in total.

The tests use other test ranges and card numbers than the lesson.
