A `late=1500` rule is fine for a person trying things by hand, but an automated test that needs an
answer to arrive *after* the client gave up depends on the machine being fast enough. On a loaded
build server the test fails now and then. QA wants the host to keep an answer back until the test
says "now", whatever the clock does.

`HoldingHost` in `src/main/java/com/example/practice/HoldingHost.java` approves every request, and
it is given the STANs whose answers the test wants to hold. Two methods are `TODO`:

- `hold(client, answer)`: keep the answer until the test releases it.
- `release()`: the test calls it from its own thread. It sends the answers held so far and returns
  how many reached a client.

`process` is called the way `ISOServer` calls it. A test may hold several answers, from several
clients, before it releases them, and a client may disconnect while its answer is held. The tests
call `process` and `release` from different threads. About 12 lines in total.
