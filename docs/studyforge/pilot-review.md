# The M10 pilot: three pages for your one review

**What this is.** Milestone M10 gives every page of this corpus exercises: authored from
what the page has, proven by gates, and graded by tests of the main ask and of each edge
case. Before the other 35 pages are authored, you review three pages once, and their shape
and counts are what the rest follow. After this review, every other page is judged by the
gates alone.

**The three pages** are the densest page of each container: the most Java examples on one
page (plan §2). **iso-fundamentals unit 15** has 10 Java examples, **jpos-client unit 8**
has 8, and **jpos-server unit 8** has 9.

**How to see them.** Open the site pages and scroll to each page's practices:

- `src/study/iso-fundamentals.unit-15-troubleshooting-and-common-issues.unit.html`
- `src/study/jpos-client.unit-08-logging-and-debugging.unit.html`
- `src/study/jpos-server.unit-08-logging-and-debugging.unit.html`

Each exercise also sits on disk, in two places:

- `exercises/<container>/prose/unit-NN/practice-M/` holds the statement, the starter, the
  worked solution, the tests, one planted wrong answer per edge case, and the gate record.
- `practice/<container>/prose/unit-NN/practice-M/` is the Maven project you would edit.

**Taken at** corpus commit `a767254` (the archive) and `455c0b4` (the site), with framework
`7f68b790`.

---

## The result in one table

| page | plan | shipped | shortfalls | fences | became exercises | written reasons |
|---|---|---|---|---|---|---|
| iso-fundamentals unit 15 | 2 | 2 | none | 10 | 5 | 5 |
| jpos-client unit 8 | 2 | 2 | none | 8 | 6 | 2 |
| jpos-server unit 8 | 2 | 2 | none | 10 (9 Java, 1 XML) | 4 | 6 |
| **total** | **6** | **6** | **none** | **28** | **15** | **13** |

Every exercise cleared all five gates on its first attempt:

- **G1:** the worked solution passes every test, twice, the same way.
- **G2:** every test fails on the starter.
- **G3:** each edge case's planted wrong answer passes the main ask and fails that edge.
- **G4:** every test in the report maps to one case.
- **G5:** the page passage it cites is unchanged.

The gates ran offline, in the pinned runner warmed with this corpus's own Maven build
(jPOS 2.1.7, JUnit 5.14.4). Each exercise is a small Maven project graded by
`mvn -o -q -f <workspace>/pom.xml test`.

⚠️ **Why the count is two on every page.** The plan sets the count from the page's length
in *prose* words, with its code left out. All three pages are in the `short` band
(250–699 words), whose ceiling is **2**. Each page teaches four or five distinct checkable
skills, so each would have planned more, and the band clamped it to two. This is the first
question below.

---

## iso-fundamentals unit 15: Troubleshooting and Common Issues

456 words of prose and 5 checkable skills: bitmap checks, field lengths, timeouts and
retries, balancing a day, and missing transactions. It plans 2.

### Exercise 1: Find where a bitmap and its message disagree

*Built from 15.1.1 "Bitmap inconsistencies".*

**The ask.** Given a primary bitmap captured as sixteen hex digits and the message it came
with, list every data element (2 to 64) where the bitmap and the message disagree.

**Edge cases:**

- Bit 1 flags a secondary bitmap and is never reported as a data element.
- A bitmap that is not sixteen hexadecimal digits is refused.

### Exercise 2: Reconcile a business day

*Built from 15.3 "Reconciliation problems".*

**The ask.** Given the network's expected total and transaction ids and your own processed
amounts by id, say whether the day balances, by how much it is out, and which expected
transactions are missing.

**Edge cases:**

- `100.0` and `100.00` are the same amount, so the day balances.
- A processed transaction nobody expected is not reported as missing.

### The page's fences

| fence | where | how it ends |
|---|---|---|
| 1, 2 | 15.1.1 Bitmap inconsistencies | exercise 1 |
| 3 | 15.1.2 Field length mismatches | reason: *the field-length validator is kept verbatim on the page; the page's two exercises went to 15.1.1 and 15.3, and the ceiling for a short page is two* |
| 4 | 15.1.2 (the processor using both validators) | reason: *it wires the two validators of 15.1 together and logs; it has no step of its own, and the ceiling is two* |
| 5 | 15.2.1 Timeout handling | reason: *it sends on a live NACChannel to a peer; a graded run has no network and no peer* |
| 6 | 15.2.2 Retry mechanisms | reason: *it wraps the live-channel timeout handler and sleeps five seconds between tries; there is no peer to retry against* |
| 7 | 15.2.2 (the transaction processor using retries) | reason: *it needs the same live channel; a graded run has no network and no peer* |
| 8, 9, 10 | 15.3.1 and 15.3.2 | exercise 2 |

---

## jpos-client unit 8: Logging and Debugging in jPOS Client

435 words and 4 checkable skills: the logging framework, a custom listener wired by Spring,
wire logging, and testing logging. It plans 2.

⭐ **A finding the page itself hides, and exercise 1 teaches it.** In jPOS 2.1.7,
`Logger.log` is static. It delivers an event only to the logger of the event's *source*.
So the page's `new LogEvent()` followed by `logger.log(ev)` reaches no listener, and the log
stays empty with no error. This was measured in the runner. Every exercise on both logging
pages therefore logs through a `LogSource`.

### Exercise 1: Log one event per call, through the logger's source

*Built from 8.1 "jPOS Logging Framework".*

**The ask.** Write a client logger that turns one call with a tag and some messages into
exactly one event, with that tag and realm, delivered to the logger it was built with.

**Edge cases:**

- A blank or missing message is left out of the event.
- A call with nothing but blank messages logs no event at all.

### Exercise 2: Log a transaction without its card number

*Built from 8.2 "Implementing Logging in jPOS Client".*

**The ask.** Log each transaction as one `Transaction` event carrying its MTI and amount,
with the card number masked. A real log must never hold a full card number, although the
page's own example logs it in full.

**Edge cases:**

- The card number is masked to its first six and last four digits.
- A message with no card number logs no card-number line.

### The page's fences

| fence | where | how it ends |
|---|---|---|
| 1, 2, 3 | 8.1.1 to 8.1.3 (Logger, LogEvent, LogListener) | exercise 1 |
| 4, 5, 6 | 8.2 (custom listener, Spring config, TransactionLogger) | exercise 2 |
| 7 | 8.3 Debugging (wire-logging channel) | reason: *the channel is built to connect to a live host and port; a graded run has no network* |
| 8 | 8.4 Testing Logging | reason: *it uses @SpringBootTest and @SpyBean from spring-boot-test, which none of the tutorial's pom fences names, so the corpus build does not carry it* |

---

## jpos-server unit 8: Logging and Debugging in jPOS Server

340 words and 5 checkable skills: setting up and creating events, a custom listener, Spring
integration, debug mode, and log rotation. It plans 2.

### Exercise 1: Leave one trace of every transaction

*Built from 8.1 "jPOS Logging Framework".*

**The ask.** Process a message and log exactly one `transaction.process` event for it. A
good message says it was processed and returns `true`.

**Edge case:**

- A message that fails still leaves exactly one event, carrying the error. This is the
  page's `finally`.

This exercise has one edge case where the others have two: the page's point on 8.1 is
exactly that one guarantee.

### Exercise 2: A debug mode you can switch off

*Built from 8.4 "Debugging Techniques".*

**The ask.** Log every transaction as a `transaction.process` event. While debug mode is on,
a `transaction.debug` event with the MTI and the amount comes first.

**Edge cases:**

- Switching debug off again stops the debug events.
- A message with no amount says `Amount: none` instead of printing `null`.

### The page's fences

| fence | where | how it ends |
|---|---|---|
| 1, 2 | 8.1.1, 8.1.2 | exercise 1 |
| 3 | 8.2 (custom listener) | reason: *it prints each event with the current date to standard output, which no test can compare; its capture-by-listener pattern is the one both exercises' tests use, and the ceiling is two* |
| 4 | 8.2 (registering it) | reason: *it only registers 8.2's listener; nothing of its own to check, and the ceiling is two* |
| 5 (XML) | 8.3 Spring integration | reason: *a Spring XML bean definition, not Java; the corpus build loads no XML context, and the listener it registers is what exercise 1 practises* |
| 6 | 8.3 (the @Service) | reason: *a field-injected service whose logging logic the page elides, so there is no behaviour to check; the ceiling is two* |
| 7, 8 | 8.4.1, 8.4.2 | exercise 2 |
| 9 | 8.5 Log rotation | reason: *RotateLogListener writes files under logs/; a graded run may write only inside its own workspace, and the ceiling is two* |
| 10 | 8.6 Unit testing | reason: *it uses AssertJ, which none of the tutorial's pom fences names, so the corpus build does not carry it* |

⭐ **Nothing on any page was removed.** Every fence above still renders on its page exactly as
the source wrote it. "Written reason" means that no exercise was built on it, not that it
went away.

---

## The questions you answer at this review

### ISO-M10/6: the bands count prose, not code

> ⚠️ **The bands measure prose only, and this corpus is code-dense.** 34 of 38 units are
> `short` (at most 2 exercises), one is `standard`, and three plan zero while carrying 7, 5
> and 5 Java fences. `jpos-client` 5 and `jpos-server` 5 hold 201 and 202 Java lines. The
> corpus ceiling is at most 34×2 + 4 = 72 exercises against 188 Java fences, so **at least
> 116 fences ship as a written reason, not as an exercise.** That is honest under the ledger
> and is exactly what the user approves at the pilot. It is recorded so the review is framed
> with it, not discovered after.

**The question:** on these three pages, 13 of 28 fences ended as written reasons.
- 4 of those 13 cite only the ceiling of two.
- 2 cite the ceiling along with another ground.
- 7 name something an offline graded run cannot have: a live peer, the network, a library
  the corpus build does not declare, or an XML context. Is that "nothing is lost" in your
sense, because every example still renders and is accounted for? Or should the bands count
code as well, so that a code-dense page gets more exercises?

### ISO-M10/7: the pilot never shows a page with its own tests

> ⚠️ **The density rule leaves case (a) out of the only review.** `iso-fundamentals` unit 4,
> the one case-(a) page with real code (9 fences), loses to unit 15 (10) by one fence. Also:
> this corpus's two test files are the framework's own M7 samples (`W426`), plain-Java and not
> JUnit, and case (a)'s blanking derivation does not exist yet (`AX-08/2`). Keep the rule, or
> substitute unit 4 for unit 15? This office keeps the rule until ruled otherwise.

**The question:** keep the densest-page rule, so no page with its own tests is ever
reviewed? Or swap iso-fundamentals unit 4 in for unit 15, so you see that case once?

### AX-10/1: on a page with tests, the source's own test file ends as a written reason

> ⛔ **In case `code-and-tests`, the source's own test file is carried by a written reason,
> even when the exercise's main ask IS that test.** An exercise has one `origin`, the worked
> draft names the page, and `accounts_for` matches on path, so `tests:checks/test_greeting.py`
> asks `excuse` for a reason. Measured on the worked corpus with `take` and `accounts_for`.
> ⚠️ Spec §7 §3 says a test file is *"the basis of at least one exercise, named by that
> exercise's origin"*. Today it can be named only by giving up the page as origin. That is
> the shape the ISO pilot will hit on every page with tests. Not in this diff.

**The question:** on this corpus it applies to iso-fundamentals units 2 and 4, whose M7
graders would be recorded as written reasons even if an exercise is built on them. Is that
acceptable? Or must the framework let an exercise name the test file as its basis first?

---

## After your answer

Your answers are recorded with the commit they were given at.

- **"Approved":** the other 35 units are authored on the gates alone (ISO-23), with the same
  shape and the same per-page reasoning, and nothing further comes back to you.
- **A change:** the pilot is re-run with it (each changed unit's directories and the ledger
  are removed, then the pass runs again), and there is still no second review.
