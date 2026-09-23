A transaction sometimes has to be undone: the terminal timed out, the customer walked away, the
response never came. The reversal (MTI 0400) only works if the other side can find the
transaction it undoes, so it must carry the same identifying data as the original.

Write `Reversals.reversalOf(ISOMsg original)`, which returns a new 0400 message carrying these
fields of the original, each only if the original has it:

- 2 (PAN), 3 (processing code), 4 (amount), 11 (STAN), 37 (retrieval reference number),
  41 (terminal ID) and 42 (card acceptor ID).

Nothing else is carried over: no response code, no entry mode, no transmission time (whoever
sends the reversal stamps field 7).

- The original is the record of what happened. Building its reversal must not change it.
- Only a transaction request can be reversed: an 0100 authorization or an 0200 financial
  request. Anything else, such as an 0110 response or an 0800 echo test, is refused with an
  `IllegalArgumentException`.
