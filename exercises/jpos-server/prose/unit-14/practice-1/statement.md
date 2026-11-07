When an issuer can't be reached, your switch approves small purchases in stand-in, up to a limit
that operations set per server through two environment variables, `STANDIN_MAX` (in minor units)
and `STANDIN_CURRENCY`. The limit is a QBean, `StandInLimit`, and its descriptor is in the comment
at the top of `src/main/java/com/example/practice/StandInLimit.java`.

Last month a new server went live without `STANDIN_MAX`. The QBean started, every stand-in
purchase was declined for a week, and nothing in `deploy/` looked wrong. Operations ask for this:

- a limit nobody set, or one that isn't a whole number of minor units, must not run, and an
  operator listing `deploy/` must see which file is wrong;
- without `STANDIN_CURRENCY`, the limit is in US dollars (`840`).

Finish `read`, marked `TODO`. About ten lines.

The tests deploy your QBean with a real Q2 in the test's JVM, on a scratch deploy directory, and
then look at the directory and at the QBean's state in JMX. Neither variable is set where they
run.
