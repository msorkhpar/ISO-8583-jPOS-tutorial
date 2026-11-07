Last night one of the acquirer's two links reconnected at 02:10 and was refused its sign-on until
06:00. For four hours every purchase went over the other link, and nobody was paged: the alert
watches the pool's `isConnected()`, and the adaptor's connection counter looked healthy too.

`PoolMonitor` in `src/main/java/com/example/practice/PoolMonitor.java` is the new alert. It is
built with the same space and the same `check-enabled` setting as the pool it watches, and
`skipped()` names the links the pool will not send to right now. Finish `inUse`, the one method
marked `TODO`. When you're done, the alert names a link exactly when the pool would pass it over,
whatever state its connection and sign-on are in. About six lines.

The tests run real QMUXes and a real `MUXPool` in one JVM. They play the adaptor and the sign-on
manager by writing to the space.
