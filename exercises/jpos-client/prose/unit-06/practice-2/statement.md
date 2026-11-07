The acquirer is launching a loyalty programme with a chain of fuel stations. Members earn and
redeem points at the pump, and the data travels in field 63. The interface document's table for
field 63 is copied into the comment at the top of `src/main/java/com/example/practice/Loyalty.java`.
The link's packager is in the same file, waiting for field 63.

`purchase` already builds the request. The redemption step, which runs first, is written too.
Finish the three methods marked `TODO`:

- `field63`: the declaration of field 63 for the link's packager;
- `addMember`: the member's programme code and member number;
- `pointsBalance`: the balance the host sends back.

When you're done, the host receives field 63 laid out as the document says, a member who redeems
points sends both the redemption and the membership, and a response for a customer who isn't a
member reads as no balance. About 14 lines in total, most of them XML.

The tests pack each message with your packager and unpack it again, as the host does.
