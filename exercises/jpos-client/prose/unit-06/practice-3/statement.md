The client's integration tests run against `HostSimulator`, in
`src/main/java/com/example/practice/HostSimulator.java`, instead of the acquirer's host. The
simulator should answer exactly as the host's interface document says: the response MTI, the
fields listed in `ECHOED`, a response code, and an approval code on approvals. The tests keep every
request they send, so that they can check afterwards what the client asked for.

`respond` is written. Finish the two methods it calls, marked `TODO`:

- `echoed`: the start of the response;
- `answerMti`: the response's MTI.

When you're done, every kind of request the client sends, first attempts and repeats, gets the
response type the real host would send, a response never carries a field the document doesn't
list, and the request a test kept still reads as the client sent it. About eight lines in total.
