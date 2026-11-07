Another acquirer joins the switch. Its terminals don't send field 90: a reversal names the purchase
it undoes by the terminal's ID (41) and the retrieval reference number (37) the terminal gave the
purchase. Each terminal makes its RRN from the date and its own counter. The fuel-card network
behind the switch keeps an approved amount on the cardholder's account until a reversal it can match
to the purchase arrives, and it ignores a reversal it can't match.

`src/main/java/com/example/practice/ReversalRecords.java` holds the participants of the forwarding
group and of the reversal group, and the builder of the upstream reversal. Finish `remember` and
`reverse`, marked `TODO`, so that a terminal's reversal frees the amount the network holds for the
purchase. Something else sends what you put on the `reversal.<link>` queues. About ten lines in
total.

The tests deploy a real TransactionManager, a real `QueryHost` and two real QMUXes with Q2, in the
test's own JVM, with a network behind each QMUX that approves everything. They look at what the
switch queued for each network and at what it keeps between a purchase and its reversal.
