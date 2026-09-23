At the end of a business day the network sends you a settlement file: the total it expects
you to have processed and the ids of the transactions it counted. You have your own record
of what you processed, id by id, with each amount.

Write `Reconciliation.check(BigDecimal expectedTotal, List<String> expectedIds,
Map<String, BigDecimal> processed)`. It returns a `Result` with:

- `balanced`: whether the processed amounts add up to the expected total;
- `difference`: the expected total minus the processed total;
- `missingIds`: the expected ids you have no record of, in ascending order.

Amounts are compared as numbers, so `100.0` and `100.00` are the same amount. Only an
expected id can be missing: a transaction you processed that the network did not count is
a different problem, and it is not reported here.
