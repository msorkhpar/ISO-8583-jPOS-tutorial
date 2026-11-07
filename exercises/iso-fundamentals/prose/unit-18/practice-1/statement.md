A terminal vendor shipped a firmware update, and since then the network has rejected a few of your
acquirer's chip authorizations as "chip data inconsistent with the message". Before you forward a
request, you want to know the same thing the network will check: does the chip data in DE 55 agree
with the ISO fields it mirrors?

`ChipConsistency.mismatches` in `src/main/java/com/example/practice/ChipConsistency.java` already
walks the four mirrored tags and collects the ones that disagree. Finish the two methods marked
`TODO`:

- `chipDigits`: the digits a chip value holds, or `null` when DE 55 doesn't have the tag;
- `agrees`: whether those digits match the field the tag mirrors, or `null` when the message doesn't
  carry that field.

The terminals sell in several currencies, refund and dispense cash as well as sell, and DE 55 holds
whatever the card and the terminal put there. About ten lines in total.
