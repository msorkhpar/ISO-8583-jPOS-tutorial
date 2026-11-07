An ATM network is adding machines in Seoul and Kuwait City to its euro machines. Each machine
pays out only whole notes of its currency, up to a maximum per withdrawal, and the server refuses
anything else before the cash dispenser is asked. The table of notes and maximums,
`CASSETTES`, is in `src/main/java/com/example/practice/CashWithdrawal.java`, and so is the
participant, which already answers every request it can read.

Finish the two methods marked `TODO`:

- `amountOf(...)`: what the cardholder asked for;
- `decide(...)`: the answer for an amount the server could read.

When you're done, every withdrawal gets a response in the Context, whatever the ATM put in its
fields, and the won and dinar machines pay out what their cardholders asked for. About fifteen
lines in total.

The tests hand `prepare` each request the way the server's channel would: packed by the ATM's
side and unpacked with `iso87ascii.xml`.
