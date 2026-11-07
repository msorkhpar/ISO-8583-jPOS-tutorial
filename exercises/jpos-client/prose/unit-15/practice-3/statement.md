Every release of the acquirer's client stops Q2, and every release leaves the same trail:

- two or three purchases that the host approved come back `null` to the payment service, which
  reverses them while their cardholders are told they failed;
- the host's operators find purchases that arrived on link `acq-mux` after it had been signed off;
- on the night of a release when the host had hung, the stop never finished, and the release was
  rolled back by hand.

`LinkStop.stop()` in `src/main/java/com/example/practice/LinkStop.java` is what the sign-on manager
runs for each link when Q2 stops it. `disable`, `signOff` and `forget` already work. Finish `stop`
and `drain`, the two methods marked `TODO`. `stop` returns how many requests were still waiting
for an answer when the sign-off went out, for the operator's log. About ten lines in total.

The tests run a real QMUX against a host on the far side of its queues, which can hold a purchase
without answering it. The `Pause` they give you takes no time.
