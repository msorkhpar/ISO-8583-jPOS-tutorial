This picks up where the previous practice ended: `src/main/java/com/example/practice/TokenVault.java`
already contains its solution. The vault issues good tokens now, but a penetration test of the
whole platform reported this:

- with the pharmacy app's own credentials, the testers got card numbers back for every token in
  its database;
- the cinema's integration sent a pharmacy token with a cinema purchase, and the pharmacy's
  customer was charged;
- a made-up token ended in a `NullPointerException` instead of a refusal the station could
  answer;
- a customer who removed his saved card was charged with it weeks later.

Finish `detokenize` and `revoke`, the two methods marked `TODO`. The comments say what each must
achieve. The station, the component that builds the request for the network, calls `detokenize`
with its permissions and the requestor it is acting for. About ten lines in total.
