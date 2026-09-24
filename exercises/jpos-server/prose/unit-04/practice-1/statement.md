A jPOS `TransactionManager` runs a transaction through its participants in the order they are
declared, in two phases. First it calls `prepare` on each in turn, and each one votes. A
participant whose vote does not carry `NO_JOIN` joins the transaction. As soon as one votes
`ABORTED`, the participants after it are not prepared at all. Then, if every vote carried
`PREPARED`, `commit` is called on every participant that joined; otherwise `abort` is called
on every participant that joined, including the one that voted `ABORTED`.

The page's flow is a validator, then a Visa participant, then a Mastercard participant, and
each network's participant steps aside for the other network's cards. That is the point: every
participant votes on the whole transaction, not just on the part it cares about, so a Visa
participant that voted `ABORTED` for a Mastercard card would abort the transaction before the
Mastercard participant were ever asked.

Write `NetworkRouting` with two nested participants (both `org.jpos.transaction.TransactionParticipant`,
working on an `org.jpos.transaction.Context`):

- `NetworkRouting.Validator`: prepares the transaction when the context holds both `"amount"`
  and `"pan"`, and changes nothing in the context when it does; otherwise it votes `ABORTED`.
  When it is aborted it puts a message under `"error"` saying the transaction was incomplete.
- `new NetworkRouting.NetworkParticipant(String network, String panPrefix)`: for a card whose
  `"pan"` starts with `panPrefix` it puts `network` under `"network"` and prepares. When it is
  committed it records that the card went to its network: it appends `network` to the
  `List<String>` under `"sent"`, creating the list if there is none yet. For any other card it
  steps aside: it must not abort the transaction, and it must not be committed for it.

The tests play the transaction manager's part, as described above, with the flow
`Validator`, `NetworkParticipant("VISA", "4")`, `NetworkParticipant("MASTERCARD", "5")`.

- A transaction missing its amount (or its PAN) aborts at the validator: it carries an
  `"error"`, and neither network marks it or sends it.
- A card is sent to its own network only: `"sent"` holds exactly that one network.
