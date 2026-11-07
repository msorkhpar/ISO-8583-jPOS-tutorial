The acquirer's host has started rejecting some purchases with response code 30, and its support
desk wants to see what your client sent. `DeclineReport` in
`src/main/java/com/example/practice/DeclineReport.java` is what the client hands over for each
rejection: an entry in the node's log, which your own team compares with the host's trace, and a
hex dump that goes by e-mail to the support desk, which works for another company.

The node's log is shipped to a platform that many people can search. Neither the log entry nor the
dump may carry card data. The support desk is hunting a format error, so the dump must still be the
message as their host received it. And the client keeps the request after reporting it, because it
may need it for a reversal.

Finish the two methods marked `TODO`: `logRejection` and `shareableImage`. About a dozen lines in
total.

The tests give the report the node's logger, with `ProtectedLogListener` in front of a writer, and
the `iso87ascii` packager. The messages carry a track 1, a track 2 and a PIN block as well as the
card number.
