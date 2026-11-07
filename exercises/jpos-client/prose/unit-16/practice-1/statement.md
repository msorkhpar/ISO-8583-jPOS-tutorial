The acquirer doesn't send its test cases as rules. It sends a sheet, one case per line:

```text
C03;0200;5555555555554444;;;54
C14;0200;;250.01;;51
C15;0200;;12.00;13.00;--
C99;0200;;;;00
```

The columns are the case, the MTI, the PAN, the amount from, the amount to, and the response code.
A blank column means "any". Amounts are written as the acquirer's analysts write them, in currency
units with exactly two decimals and nothing else (`250.01`). A response code is two letters or
digits, and `--` means the host doesn't answer. The acquirer's notes add one sentence:
the host goes through the rows in the order they are listed.

QA wants to load the sheet as it comes, instead of retyping it into rules.
`src/main/java/com/example/practice/SheetHost.java` already reads the lines and answers, but
three methods are still `TODO`:

- `matches(row, request)`: does this row apply to the request?
- `decide(request)`: which row answers it?
- `check(row)`: the sheet is typed by hand, and a row the simulator can't read exactly as the
  acquirer means it must stop the sheet from loading, with an `IllegalArgumentException`.

The tests load sheets you haven't seen, with amounts at the edges of their ranges. About 15 lines
in total.
