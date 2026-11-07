Every night support exports its tickets to an analytics vendor. Before the vendor is allowed to
receive them, the compliance team wants a report of the tickets that hold card numbers, so that
those can be cleaned first and the vendor can stay out of scope.

Customers type card numbers the way they read them off the card, in groups, with spaces or dashes
or neither. Support staff paste lines from terminal logs. Tickets also hold dates, phone numbers,
order numbers and case references.

`CardNumberFinder.find` in `src/main/java/com/example/practice/CardNumberFinder.java` already walks
the ticket and builds the report. Finish the three methods marked `TODO`:

- `candidates()`: what in a ticket might be a card number;
- `isCardNumber(...)`: which candidates are one;
- `shown(...)`: how a card number appears in the report.

When you're done, the report lists every card number in a ticket, where it starts, and in a form
that is safe to hand around, and nothing else. About ten lines in total.
