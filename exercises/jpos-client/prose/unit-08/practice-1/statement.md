Some hosts check a quiet link by sending echoes of their own: an 0800 that your client must answer
with an 0810. `EchoResponder` in `src/main/java/com/example/practice/EchoResponder.java` does that
job as a request listener on the client's QMUX. Its class comment shows the `<request-listener>`
element that deploys it.

Last week the host's operations team reported that your client had stopped answering their echoes
for an hour. Your log had nothing on it, so nobody could say whether the echoes arrived, whether
replies went out, or what failed. Operations now want one entry per echo in the node's log, under
the listener's realm, that shows what came in and what went back, or why nothing did.

Finish the two methods marked `TODO`: `setLogger` and `answer`. About ten lines in total.

The tests give the listener its logger the way Q2 does, then hand it echoes from a link that
records the replies and, in one test, fails to send.
