Hotels and fuel pumps send pre-authorizations (`0100`), and for each one your server places a hold
on the cardholder's account. When the stay is cancelled, or the pump never got an answer, the
acquirer reverses the pre-authorization with an `0400`, and repeats it as `0401` until it is
answered.

The complaints: guests still see a hotel's hold days after the desk cancelled it; one cardholder
had the same hold given back twice; and the acquirer's operations team asks why some reversals
come back refused.

`ReleaseHold` in `src/main/java/com/example/practice/ReleaseHold.java` answers those reversals.
Its `HoldTable` (at the bottom of the file) is the table the authorization chain fills. Finish
`lift` and `answerCode`, marked `TODO`. About seven lines in total.

The tests deploy a real TransactionManager with Q2, in the test's own JVM, place holds in the table
the way the authorization chain would, and send reversals from several terminals.
