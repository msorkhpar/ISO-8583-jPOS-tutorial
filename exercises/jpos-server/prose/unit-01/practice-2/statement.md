Settlement reconciles the day against a journal: one line, with terminal, STAN and amount, for
every purchase the server approved. After a busy Saturday the journal had a few lines twice and a
few purchases not at all, and one line for a purchase whose transaction had failed after the
approval.

`ApprovalJournal` in `src/main/java/com/example/practice/ApprovalJournal.java` sits in the chain
after the authorizer, which has already put its response in the Context. Other participants come
after it. Finish `prepare` and `commit`, marked `TODO`. A purchase (`0200`) the authorizer approved
(`39` = `00`) gets exactly one line, and only if its transaction commits; anything else gets none.
The journal is given (`journal()`). About eight lines in total.

The tests deploy real TransactionManagers with Q2, in the test's own JVM. One of them runs two
sessions and holds two purchases side by side until both are past your `prepare`.
