The host's operators have written to you twice.

- During last Tuesday's maintenance window your client sent them a sign-on every second for twenty
  minutes, and they would like that to stop.
- After a night-long outage, when the host came back at 06:10, your client was among the clients
  that hammered it with sign-ons, though it had been quiet for most of the night.

Their rules of connection: after a refused or unanswered sign-on, wait `firstWait` before the next
one, and twice as long after each further refusal in a row, but never longer than `longestWait`.
After an accepted sign-on, the next outage starts afresh.

The sign-on manager asks `maySignOn(now)` before each sign-on and reports every outcome to
`LinkPolicy` in `src/main/java/com/example/practice/LinkPolicy.java`. Finish `refused` and
`signedOn`, the two methods marked `TODO`. About eight lines in total. Times are milliseconds and
the caller passes them in, so the tests play a whole night in a moment.
