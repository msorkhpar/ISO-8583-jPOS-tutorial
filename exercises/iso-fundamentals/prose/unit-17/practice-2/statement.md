The acquirer is moving its e-commerce traffic to the network's 2023 link, and the network's
certification team sends back its first findings: purchases with a long authentication payload
arrive unreadable, and one test message made the gateway throw instead of sending anything. The
gateway builds DE 34 and DE 104 itself, because jPOS 2.1.7 carries them as plain bytes.

`CompositeWriter.write` in `src/main/java/com/example/practice/CompositeWriter.java` already loops
over the datasets. Finish the two methods marked `TODO`:

- `content`: a TLV dataset's elements, as bytes;
- `envelope`: the dataset, as it goes into the field.

Tags are given as numbers, such as `0x9C` or `0xDF8117`, and are written with as many bytes as they
need. Refuse with an `IllegalArgumentException`. About twenty-five lines in total.
