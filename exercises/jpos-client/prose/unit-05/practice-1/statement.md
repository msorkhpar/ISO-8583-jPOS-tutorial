Your gateway builds purchases for many merchants. Each merchant arrives with the properties file
the acquirer's boarding team filled in: terminal, merchant ID, category code, currency, name, city,
country, and one `acquirer.<network>` line per card network the merchant may accept. Since the
gateway started taking Mastercard and Discover cards, the acquirer has reported two kinds of bad
purchases: some reach it with no acquiring institution ID, or with the wrong network's, and some
show merchants in cities that don't exist.

`Boarding.purchase` in `src/main/java/com/example/practice/Boarding.java` already builds the
request. Finish the two methods marked `TODO`:

- `acquirerId(network)`: the merchant's ID for that network. When the merchant has none, the
  purchase must not be built: throw `IllegalArgumentException`.
- `nameAndLocation()`: field 43 for any merchant the boarding team may send you, short or long.

The tests pack each purchase with the course packager and read field 43 back as a host does.
About ten lines in total.
