The acquirer forwards Mastercard purchases to the network with `QueryHost`. Cardholders write in:
the terminal said the purchase failed, they paid again another way, and their bank still shows a
hold for the first attempt. On the same days, a few terminals waited their full timeout and got
nothing at all. The Mastercard link also goes down for maintenance now and then.

`AfterNetwork` in `src/main/java/com/example/practice/AfterNetwork.java` stands right after
`QueryHost` in the `mastercard` group. After the group the chain has an abort participant that
answers reported failures (as in the unit) and `SendResponse`. Finish `prepare`, marked `TODO`, so
that every purchase ends with the right answer at the terminal and nothing left wrong at the
network. Something else sends what you put on the `saf` queue, and `reversalOf` builds the message.
About eight lines.

The tests deploy a real TransactionManager, a real `QueryHost` and two real QMUXes with Q2, in the
test's own JVM. A scripted network on the far side approves, declines or never answers. One QMUX's
link is down.
