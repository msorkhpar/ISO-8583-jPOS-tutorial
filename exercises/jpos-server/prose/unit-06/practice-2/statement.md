A card network asks its acquirers to stop processing requests whose transmission time (field 7)
is more than five minutes away from their own clock: replays, and requests from terminals whose
clocks have drifted. Such a request is answered with `30` and goes no further. The participant
that does it is `TransmissionWindow`, in
`src/main/java/com/example/practice/TransmissionWindow.java`. It also leaves the sending time in
the Context under `SENT_AT`, for the participants after it.

`prepare` already works. Finish the two methods marked `TODO`:

- `sentAt(...)`: the moment field 7 names;
- `withinWindow(...)`: whether that moment is close enough to the clock.

When you're done, the server refuses exactly the requests the network wants refused, on every day
of the year and wherever it is deployed, and answers each one. About five lines in total.

The tests give the participant a fixed clock, and some of them change the JVM's default zone.
