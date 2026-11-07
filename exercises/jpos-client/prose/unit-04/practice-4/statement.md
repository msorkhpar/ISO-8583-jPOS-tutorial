`PaymentService` sends purchases through whatever Q2 has deployed under a name it is given, such as
`acquirer-mux`. It is created when the application starts and then runs for weeks. In that time the
operations team edits the descriptors: they change a timeout and Q2 redeploys the file, or they
add a second link and give the old name to a pool. They expect the service to keep working through
all of it without a restart.

Write `mux()` in `src/main/java/com/example/practice/PaymentService.java`. `pay(...)` already
handles a `null` from it as "unavailable". About five lines.

The tests do what Q2 does with a descriptor, without a Q2: they create the component, register it
under its name, start it, and stop and destroy it on a redeploy.
