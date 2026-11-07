You run the issuer's authorization host for a credit card programme. `Account` keeps one
cardholder's account, and its `authorize` already answers the network's `0100`s. Two kinds of
complaint keep reaching the call centre:

- after a stay or a fill-up, part of what the merchant first asked for still shows as blocked,
  long after the final charge has posted;
- after last Saturday's outage, cardholders whose purchases went through during it got further
  approvals afterwards, and their accounts ended up below zero once everything cleared.

Finish `standIn` and `clear` in `src/main/java/com/example/practice/Account.java`. When you're
done, `ledger()` and `available()` match what each cardholder has really spent and committed to,
whoever approved the purchase and whatever the clearing file says. About six lines in total.

The tests use other cards, amounts and approval codes than the lesson, including a purchase a
terminal approved on its own.
