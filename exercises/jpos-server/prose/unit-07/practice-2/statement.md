The network now signs its answers, with a MAC key it shares with us (`zak.network`). The terminals
check ours, each under its own key (`tak.` plus its terminal ID). Since the change, the terminals'
support line hears two complaints: some of our answers fail the terminal's check, and the network
team asks why we passed on an answer of theirs that someone had changed on the way.

`src/main/java/com/example/practice/NetworkAnswers.java` holds two participants. `CheckNetworkMac`
stands right after `QueryHost`; `SignForTerminal` stands right before `SendResponse`. Between them,
an abort participant answers `91` when it finds no response in the Context. Finish the parts marked
`TODO`. `mac(message, key)` and `key(alias)` are given. About fifteen lines in total.

The tests deploy a real TransactionManager, `QueryHost` and QMUX in the test's own JVM, with a real
software security module. The network on the far side signs, declines, or sends answers that are
not what it signed.
