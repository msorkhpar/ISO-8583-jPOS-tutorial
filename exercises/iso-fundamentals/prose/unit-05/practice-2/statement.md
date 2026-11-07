Merchants compare the acquirer's end-of-day report with their tills, and a coffee chain has opened a
ticket: on some days the report shows more sales than its tills, on others fewer. The acquirer
reaches Visa over both kinds of link, and its terminals send every kind of account their cards
offer.

`DailyReport` in `src/main/java/com/example/practice/DailyReport.java` already walks the messages the
acquirer sent and adds up the amounts per kind. Finish the three methods marked `TODO` that decide
what each message is:

- `undoesAnother`: whether a message cancels an earlier one;
- `asksAboutMoney`: whether a message is about a transaction with the cardholder's money at all;
- `byProcessingCode`: purchase, cash or refund, from field 3.

About ten lines in total.
