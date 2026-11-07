Two gateway processes now share one terminal ID on the link to `acquirer-b`, so they need one
sequence of STANs between them, and a Space in each process won't do. The sequence lives in a
database table instead: one row per link, shared by both processes, and still there after a
restart. The given `Table` class at the bottom of
`src/main/java/com/example/practice/StanCounter.java` stands in for it. Each of its methods is one
SQL statement.

Write `next()` in that file. The acquirer counts the gateway's duplicates and collisions, and it
wants none, today or on the day the sequence passes 999999. About six lines.

One of the tests makes both processes read the row at the same moment before either writes.
