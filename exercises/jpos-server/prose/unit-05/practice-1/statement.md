The merchant's contracts differ by brand. Visa purchases may be in euro (978), US dollars (840) or
pounds (826). Mastercard purchases may only be in euro, and the acquirer wants those refusals
answered with 57 rather than the usual 58. Last quarter a property name misspelt in a descriptor
left a check like this one switched off for a week, and nobody noticed.

`AcceptedCurrencies` in `src/main/java/com/example/practice/AcceptedCurrencies.java` sits in both
brand groups of the TransactionManager, configured differently in each. Finish `setConfiguration`
and `prepare`, marked `TODO`. The comment at the top of the class lists the properties. About a
dozen lines in total.

The tests deploy a real TransactionManager with Q2, in the test's own JVM, with
`SelectDestination`, `Switch` and a `visa` and a `mastercard` group. They send purchases of both
brands in several currencies.
