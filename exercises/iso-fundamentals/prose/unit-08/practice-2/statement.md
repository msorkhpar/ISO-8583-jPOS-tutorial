We are joining a regional debit network. Its MAC is the retail MAC, but its specification pads the
MAC input differently from the links we had so far. The relevant section is copied into the class
comment of `src/main/java/com/example/practice/NetworkMac.java`.

In the network's test window, some of our messages were refused as wrongly MACed, and their
security team asked how our side treats the MACs it receives: their test script sends answers with
a MAC that is wrong, shortened or missing, and expects every one of them to be refused.

`mac` already runs the algorithm; it calls `pad` for the MAC input. Finish the two methods marked
`TODO`:

- `pad(data)`: the MAC input as the network's specification pads it;
- `verify(key16, data, received)`: whether `received` is the MAC of `data`.

About eight lines in total. The tests compare your MACs with the network's own reference
implementation.
