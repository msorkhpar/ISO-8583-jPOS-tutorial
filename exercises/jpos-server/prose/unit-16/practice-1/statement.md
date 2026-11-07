The switch now also forwards purchases and balance inquiries from fuel-station terminals to `fleet`,
a fuel-card network, as well as to `visa`. The fleet network knows the switch as one terminal,
`SW000001`, sent by acquirer `12345678901`, and refuses any message that carries the terminals'
private field 48. The terminals match answers the way any QMUX does. A purchase's approval must show
the network's approval code, and an inquiry's the balance the network returned (field 54). Nothing
else in the network's answer is for the terminal.

`ForwardLeg` in `src/main/java/com/example/practice/ForwardLeg.java` already holds the two
participants that stand around `QueryHost` in the forwarding group. Finish the three methods marked
`TODO`: `outbound`, `reply` and `nextStan`. About fifteen lines in total.

The tests deploy a real TransactionManager, a real `QueryHost` and two real QMUXes with Q2, in the
test's own JVM, with a scripted network behind each QMUX. After the group, an abort participant
answers reported failures from the terminal's request, as unit s10's does. Besides approvals, the
tests send declines, purchases the network never answers, and two purchases in flight at once.
