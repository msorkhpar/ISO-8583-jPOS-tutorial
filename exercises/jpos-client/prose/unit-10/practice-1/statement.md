The payments team tests its clients against `ScriptedMux`, a stand-in host in
`src/main/java/com/example/practice/ScriptedMux.java`. A test scripts the next answers with
`then(...)`: a response code such as `"00"` or `"51"`, or `ScriptedMux.SILENT` for a host that
says nothing. Once the script runs out, the host approves.

Last month two bugs reached production although tests had been written for both. A client that
sent every unanswered authorization a second time passed the test that was meant to catch it, and
a client that kept the 0100 it sent, to build a reversal from it later, found an 0110 there
instead. Both times the fake was the culprit, and its answering part is now blank.

Finish the three methods marked `TODO`: both forms of `request`, and `answer`, which builds the
response. When you're done, a client tested against `ScriptedMux` meets the same behaviour it will
meet behind a real QMUX, for authorizations, reversals and anything else it sends. About a dozen
lines in total.
