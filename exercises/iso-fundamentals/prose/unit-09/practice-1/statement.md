Your gateway connects 1987 terminals to a processor that answers with four-digit action codes.
`CodeTranslator` holds a table, written code by code from both networks' lists, and tells each
terminal what the processor decided. `answerFor` and `mustReverse` already work on top of
`toTerminal`.

The processor's release notes add codes faster than the table is updated, and during last week's
upgrade some answers arrived with no action code at all. Operations asks for three things: no
terminal hands over goods the issuer may not have approved in full; no cardholder is told their
card was refused when it was a system that failed; and no purchase is reversed when the processor
plainly refused it.

Finish `toTerminal` in `src/main/java/com/example/practice/CodeTranslator.java`. About ten lines.

The tests use a table of their own and codes it doesn't list.
