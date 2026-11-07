Your issuer has started sending scripts to unblock PINs and block lost cards' applications. In the
test lab some terminals answer every response that carries a script with "chip data format error",
and the scripts never run.

`IssuerResponse.de55` in `src/main/java/com/example/practice/IssuerResponse.java` writes the
response's DE 55 byte by byte, without `TLVList`, as the network's reference implementation does.
Tag `91` already goes out. Finish the two methods marked `TODO`:

- `length`: the bytes that announce a value's length;
- `template`: one script, ready for the card.

The scripts come in every size the network allows, with or without an identifier. About twelve lines
in total.
