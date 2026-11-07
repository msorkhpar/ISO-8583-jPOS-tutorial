Your gateway sits between a few hundred terminals of different makes and the acquirer. The
acquirer's fraud team has written to you: some of your requests say one thing in field 22 and
another in the card fields after it, and they want those stopped at the gateway, answered with
response code 30, instead of reaching their host. The first example they attached is a chip read
with no chip data. It is not the only kind.

`EntryModeScreen.screen` in `src/main/java/com/example/practice/EntryModeScreen.java` already
answers the terminal when there is a problem. Finish `problem(...)`, marked `TODO`, so that it
finds every request whose read and card data disagree, and only those. The codes the acquirer's
specification lists are constants at the top of the class. Your terminals are not all alike: some
have a PIN pad and some don't.

About ten lines.
