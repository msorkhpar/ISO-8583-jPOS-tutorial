Cardholders can pay part of a purchase with loyalty points. `RedeemPoints`, in
`src/main/java/com/example/practice/RedeemPoints.java`, takes the points off the loyalty account in
`prepare` (field 48 says how many, field 102 names the account), and the loyalty service holds them
until the server confirms or cancels the redemption. A purchase without field 48 uses no points.

Last month the server was restarted after a crash, and the loyalty team found redemptions that were
neither confirmed nor cancelled, for purchases that never got an answer. The points are still gone
from the cardholders' accounts.

The TransactionManager already journals to disk. Finish the two methods marked `TODO`:

- `afterRedeem`, called once the service has redeemed the points;
- `finish`, which `commit` and `abort` call.

About ten lines in total. The service is the `PointsService` interface at the top of the file.

The tests deploy a real TransactionManager with Q2 in the test's JVM, with a Berkeley DB journal.
They play a crash by pausing a transaction at a later participant and never resuming it, then stop
the TransactionManager and start it again, so that it recovers from its journal.
