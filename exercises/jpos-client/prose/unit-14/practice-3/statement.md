A hotel chain pre-authorizes each stay at check-in (an 0100 that places a hold on the card) and
completes it at check-out with an 0220 whose field 90 points at the 0100. The acquirer runs two data
centres, and the chain has one link to each. The data centres don't share their holds. Since the
second link went live, some guests are charged at check-out while the hold from check-in stays on
their card for a week, and the acquirer's answer to some completions is "original not found".

`HoldRouter` in `src/main/java/com/example/practice/HoldRouter.java` is the chain's own `MUX` in front
of the two links. It already sends every message out in turn. Finish the three methods marked
`TODO`:

- `remember(...)`: what the router keeps when a pre-authorization goes out;
- `holdOf(...)`: how a completion finds it again;
- `forCompletion(...)`: where a completion goes now.

Completions that get no answer stay in the chain's advice queue, which calls `request` again later.
About ten lines in total.

The tests run two real QMUXes with a test host behind each, one per data centre.
