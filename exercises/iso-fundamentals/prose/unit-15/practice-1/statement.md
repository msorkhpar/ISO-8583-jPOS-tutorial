Your switch's support desk answers tickets from several acquirers. Each ticket gives a terminal
ID, a STAN as the analyst typed it, and the moment the acquirer's records say the message was sent.
The desk pastes the day's log into `TicketSearch.find`, which returns every logged message the
ticket is about: the request and its answer, in log order, or nothing when the log never saw it.

`find` is written. Finish the three methods marked `TODO` in
`src/main/java/com/example/practice/TicketSearch.java`:

- `stan(...)`: the ticket's STAN as a message carries it;
- `sent(...)`: the ticket's moment as a message carries it;
- `names(...)`: whether a logged message is one the ticket is about.

About six lines in total. The tests' logs hold terminals in more than one country, more than one
day, and STANs that come round again; some tests run on a machine whose clock is not set to UTC.
