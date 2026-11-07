After Tuesday's deploy, three cardholders called the bank. Their terminals had printed FAILED,
and their statements show the purchases charged. The switch restarted twice during the deploy,
both times while purchases were timing out. Reconciliation also found a purchase that the journal
lists as being reversed, although no reversal for it ever reached the host. And on Wednesday a
cardholder was charged for a purchase that the terminal had reported as failed, after the host's
approval arrived a minute late.

`TimeoutHandler` in `src/main/java/com/example/practice/TimeoutHandler.java` decides what happens
once a purchase sent upstream has ended. `onAnswer` is written. Write `onNoAnswer` and
`onLateAnswer`; the comment on each says what it must achieve. `Db`, at the top of the file, is the
switch's database with its journal and its store-and-forward queue. Changes made in a `Tx` become
real together when it commits.

The tests make the process die after a chosen step (a commit, or a message to the terminal) and
then look at what survived. About 10 lines in total.
