An ATM network is moving its machines to a new acquirer. Each ATM builds its requests with
`AtmRequests`, in `src/main/java/com/example/practice/AtmRequests.java`: balance inquiries,
cash withdrawals, and the sign-on it sends when it comes into service. The machines stand in
several countries, each with its clock set to local time, and they stay in service for years.

The two card requests are written. Finish the four methods marked `TODO`; the comment on each says
what it is for. The acquirer's test team will compare what your messages say with what it receives
and what it echoes back, from ATMs in different time zones. About 10 lines in total.
