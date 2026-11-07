An integration test checks that a reversal leaves the store-and-forward queue and reaches the
host. The SAF sends on its own thread, so the test asks the host's log for it:
`awaitReceived("0420", 5000)`. The test host puts every message it receives, in order, on a Space
queue, and `HostLog` in `src/main/java/com/example/practice/HostLog.java` reads that queue.

The test fails about once in twenty runs on the build server and never on a laptop. Nobody has
found the cause, so the team re-runs the build until it passes. `awaitReceived` sleeps half a second
and then looks once.

Rewrite `awaitReceived`, the one method marked `TODO`. It returns the next message of the asked
type, as soon as the host has it, and gives up when the time the test allowed is over. Network
messages such as echoes can reach the host at any moment and are not what the test is waiting
for. When the message doesn't come, the failure must say so in the test that waited, not as a
`NullPointerException` three lines later. About ten lines.

Some of the tests deliver a message only after your code has started looking for it.
