The operations dashboard shows one line per host link, and the people on call act on it: UP means
leave it alone, DOWN means call the network team, NO_ANSWER means call the host's support desk,
and NOT_DEPLOYED means our own deployment is broken. Last month it said UP for an hour while every
payment failed, and the month before it sent the network team after a host that was simply slow.

`LinkStatus.check()` in `src/main/java/com/example/practice/LinkStatus.java` runs every few
seconds for as long as the application runs. Q2 deploys the link's multiplexer when it starts
and replaces it whenever someone edits its file in `deploy/`. `check()` and `echo()` are written.
Finish the two methods marked `TODO`:

- `find()`: the multiplexer to probe;
- `probe(mux)`: what the link behind it is doing.

About 12 lines in total.

The tests deploy a real QMUX under the link's name, the way Q2 does, with a stand-in for the
channel adaptor and a host behind it. Some of them redeploy it between two checks.
