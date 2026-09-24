# ISO-23 handoff: all 38 units, planned by their important ideas (W453)

**Office** `po-int`. **Branch** `int/m10-iso-23`, from `424e55a` (ISO-26's tip). **Framework**
`6d0b8dc6` (W453), re-pinned through `reonboard` (`38b1dff`) and exported with `git archive`,
never checked out. **Runner** `code-server-toolchain/runner:java-maven-amd64-54f18c498a95`,
offline (`--network none`), one fresh container per gate run, under the shared container lock.
`workspace.json` was not touched: the register re-pins and deploys.

The commits, in order:

| commit | what |
|---|---|
| `38b1dff` | re-pin to `6d0b8dc6` through `reonboard` |
| `a87573f` | clear the plan_api 1 data (the pilot's bundles, the unit-1 quiz, their workspaces, the ledger), and re-emit the archive |
| `1dc8bdc` | batch 1: `iso-fundamentals`, 16 units |
| `b137d11` | batch 2: `jpos-client`, 11 units |
| `6b16acd` | batch 3: `jpos-server`, 11 units |
| `a3936b6` | emit through the adapter, rebuild the site, re-address the `tests/ingest` fixtures |
| `0655069` | `m10-plan.md` (the band column, the ISO-23 row and its result, findings `ISO-M10/11`–`14`) and this handoff |
| `dda566b` | `jpos-client` unit 1 re-planned as zero, by the register's ruling on `ISO-M10/12` |
| this commit | the documents follow the ruling; F10 settled |

## The rule, and how it was applied

The user's ruling of 2026-09-23 (framework `W453`), quoted in full in `m10-plan.md`'s ISO-23
row, works like this:

- Each page is planned by its **important ideas** (aspects), prose and code. Related ideas share
  one exercise.
- An idea left unpractised carries a written reason, such as "incidental", "needs a live peer",
  or "practised on page N".
- Trivia (dates, host names, port numbers, boilerplate wiring) is not an aspect.
- There is no band, no ceiling and no quota.

Most pages ended with one exercise, seven with two, and three with none by plan.

- `jpos-client` unit 11 (connection management) plans zero. Its one real idea, pooling channels,
  is practised on unit 3 (`pooled-channels`), and a second exercise on it would repeat unit 3.
- `jpos-server` unit 1 (basic setup) plans zero. Its ideas are Q2 hosting Spring and jPOS parts
  wired as beans. Both are configuration, and they show only in a running Q2.
- `iso-fundamentals` units 1 and 16 are quiz pages, with two questions each (the user: *"for sure
  4 questions were a lot"*). Unit 1 keeps the version question and drops the dates.
- `iso-fundamentals` units 2, 3 and 4 carry the source's own `practice-1`, so their authored
  exercises start at `practice-2`.

## Result

| container | units | code | quiz | zero by plan | shortfall |
|---|---|---|---|---|---|
| `iso-fundamentals` | 16 | 17 | 2 | 0 | 0 |
| `jpos-client` | 11 | 12 | 0 | 2 (units 1, 11) | 0 |
| `jpos-server` | 11 | 11 | 0 | 1 (unit 1) | 0 |
| **total** | **38** | **40** | **2** | **3** | **0** |

The exercises per unit, in practice order:

- `iso-fundamentals`:
  - 1 quiz `check-yourself`
  - 2 `read-both-bitmaps`
  - 3 `encode-a-field`
  - 4 `match-the-response`, `reverse-a-transaction`
  - 5 `visa-purchase-participant`
  - 6 `build-mastercard-requests`
  - 7 `authorize-through-mux`
  - 8 `pin-block`
  - 9 `handle-the-outcome`
  - 10 `simulate-the-host`
  - 11 `record-transactions`
  - 12 `bridge-to-a-gateway`
  - 13 `mask-for-display`
  - 14 `chip-data-tags`, `tokenize-pan`
  - 15 `bitmap-mismatches`, `reconcile-day`
  - 16 quiz `check-yourself`
- `jpos-client`:
  - 1 none (by plan, register ruling on `ISO-M10/12`)
  - 2 `request-types`
  - 3 `pooled-channels`
  - 4 `send-over-mux`
  - 5 `card-network-requests`
  - 6 `answer-a-request`, `subfields-and-binary`
  - 7 `secure-exchange`
  - 8 `logger-through-source`, `masked-transaction-log`
  - 9 `error-responses`, `reply-outcomes`
  - 10 `simulated-host`
  - 11 none (by plan)
- `jpos-server`:
  - 1 none (by plan)
  - 2 `message-templates`
  - 3 `answer-requests`
  - 4 `network-participants`
  - 5 `check-by-card-network`
  - 6 `answer-from-the-request`
  - 7 `pin-block-into-field-52`
  - 8 `one-trace-per-transaction`, `switchable-debug`
  - 9 `store-each-transaction`
  - 10 `errors-become-response-codes`
  - 11 `screen-a-valid-amount`

The three pilot units (`iso-fundamentals` 15, `jpos-client` 8, `jpos-server` 8) still fit their
pages, so their exercises were carried over. Against `424e55a` the pilot's bundles differ only in
two places:

- each unit's `coverage.json`, which now holds the `W453` plan;
- the `pom.xml` of `iso-fundamentals` unit 15 practice-2, and the gate record over it.

The three pilot pages are byte-unchanged.

### `jpos-client` unit 1: a zero plan, by the register's ruling on `ISO-M10/12`

`src/c1.md` teaches two ideas: the crossing `send`/`receive` queues between the channel adaptor
and the mux, and application code finding the mux by name. It was first planned as a
two-question quiz. `Q1` held on every attempt and `Q3` refuted every wrong option, but `Q2`
refused all three attempts: both page-free readings picked both keys in attempts 1 and 2, and
attempt 3 resubmitted attempt 2 unchanged. Both ideas are standard jPOS convention, so general
knowledge answers them, and no honest quiz makes the page necessary.

⭐ **The register ruled (reversibly, as corpus data) that the unit plans zero** (`dda566b`).
Every aspect and every unexercised fence carries that reason, and the unit has no shortfall. To
reverse it, give the unit's aspects an exercise again in the plan, remove only the unit's
`exercises/` directory (and its `practice/` workspace, if any), and run the pass. ⛔ Never delete
`exercises/ledger.json`: that is what caused `ISO-M10/11`, and a framework with `W456` merges the
ledger in place.

## The gate records

Each shipped bundle holds its `gates.json`: G1–G5 for code, and Q1–Q5 for a quiz. `Q1`–`Q3`
were taken over each question's digest by independent model sessions, never the author:

- `Q1` twice, reading the page only;
- `Q2` twice, with no page and no tool, and allowed to answer `NONE`;
- `Q3` once per wrong option, naming a section and quoting it.

`Q4` and `Q5` were re-run mechanically. Two quiz attempts were refused and re-authored:

- `iso-fundamentals` unit 16: attempt 1 was refused at `Q2` (a page-free reading picked the
  data-model key). It cleared at attempt 2.
- `jpos-client` unit 1: refused at `Q2` three times, and now a zero plan (above).

Every code exercise was rehearsed before the pass (the reference passes every case, the starter
fails every case, and each plant passes the main case and fails its own edge). The pass then ran
the real gates.

## Proof

All taken at `a3936b6` in this worktree, host `python3`, with the framework exported at
`6d0b8dc6`:

- `python3 -m ingest . 2026-09-21`, then `studyforge build . --out .`: both exit 0.
- `studyforge validate .`: **GREEN, exit 0** (0 findings, 0 unchecked claims).
- `python3 -m pytest tests`: **GREEN, exit 0**. It was RED (exit 1, 5 failed) before the fixture
  fix (`ISO-M10/14`).
- Ledger: 220 entries (218 lesson fences and the 2 declared graders), each covered by an
  exercise's origin or a written reason. There are 38 `coverage.json` files.
- M7: `practice/bitmap`, `fields` and `mti`, and the archived `practice-1` of `iso-fundamentals`
  units 2, 3 and 4, are byte-unchanged against `424e55a`.
- Site: 32 of the 38 unit pages changed. The 6 unchanged pages are the three pilot pages and the
  three pages that ship nothing.
- Browser: served on a spare loopback port, never `:8770`, and read in headless Chrome over CDP.
  - Quiz, `iso-fundamentals` unit 1: answered `q-2003` right and `q-roles` wrong. The page POSTed
    `/api/v1/quiz/iso-8583-jpos-tutorial/iso-fundamentals/unit-01/practice-prose/q-2003=c/q-roles=a`
    (200) and showed the right sentence, the wrong one, and "1 of 2 answered correctly".
  - Code, `iso-fundamentals` unit 5 (`visa-purchase-participant`): Submit on the starter POSTed
    `/api/v1/run/…/test/…` (200) and showed "Main ask: not yet. Edge cases 0 of 2", all four
    cases "Not yet". With the bundle's reference in the workspace it showed "Main ask: done. Edge
    cases 2 of 2", all four cases "Done". The starter was then restored with `git checkout`.
  - ⚠️ The code was graded in **host mode, through a scratch `mvn` shim** that runs the pinned
    runner image offline. The serve looks up its runner by a container name fixed per `source`,
    and that container is the user's, over the main checkout (`ISO-M10/13`). The editor POST
    returned 404 because no editor ran for this serve; that is expected.

## Findings (none patched in `studyforge`)

The framework rows are in `m10-plan.md` §4:

- `ISO-M10/11`: a partial pass rewrites the corpus's one ledger, dropping every other page's
  entries, and `validate` stays GREEN.
- `ISO-M10/12`: a page that teaches only a tool's convention cannot clear `Q2`.
- `ISO-M10/13`: a second checkout cannot grade code without the user's runner container.
- `ISO-M10/14`: fixture assumptions; local, and fixed here.
- `ISO-M10/6` is withdrawn by `W453`.

⭐ **Settled: F10** (the quiz key reachable by URL) was fixed by framework `W452`, merged at
`a394fb5b` and live.

Carried from ISO-25 and ISO-26, still open:

- **F4**: `validate` does not re-run `Q5`.
- **W453/1**: an aspect's link to its exercise is recorded, not gated.
- `Q2` is unstable between readings, which is why it is taken twice.

**Page defects the authors found.** ⭐ **The user ruled "fix them" (2026-09-23), and `ISO-27`
fixed every one** of those listed below; see `handoffs/ISO-27.md` for each page's fix and proof.
At ISO-23 the pages were left unchanged, and each exercise taught what jPOS 2.1.7 actually does,
verified by running.

- `src/6.md`: two-digit processing codes.
- `src/7.md`: chargeback uses `0400`.
- `src/8.md`: the PIN block formats are wrong, and the DUKPT shown is not the standard algorithm.
- `src/11.md`: calls `getPayload`.
- `src/12.md`: field 52 is used for a CVV; the code depends on the locale and on `LocalDate.now()`.
- `src/13.md`: `ISOUtil.protect` masks with `_`, which the page does not reflect.
- `src/c1.md`: the `QMUX` bean does not compile as written.
- `src/c2.md`, `c3.md`: `ISOClient` and `MsgFactory` do not exist in jPOS 2.1.7.
- `src/c4.md`: the MUX API differs from 2.1.7.
- Several `c*` pages use AssertJ and `spring-boot-test`, which the offline run does not carry.
- `src/s2.md`: `MsgFactory` and `ISOFactory` are missing, and `pack` throws a
  `NullPointerException`.
- `src/s3.md`: calls `NACChannel` SSL.
- `src/s4.md`: the Visa participant aborts every Mastercard transaction.
- `src/s5.md`, `s6.md`: `prepare(long, Context)` does not compile on 2.1.7.
- `src/s7.md`: `SMAdapter` method mismatches.

**One gate caveat.** `iso-fundamentals` unit 14's `tokenize-pan` edge plant can pass by chance,
with a probability of about 1 in a million per run. It cleared, and it is recorded here so a
future red on it is not a mystery.

## For the register

- **Merge**: this branch is based on `424e55a` (ISO-26), so ISO-25 and ISO-26 come with it.
- **Re-deploy**: the register's. The site at `:8770` is the user's and was not touched.
- **Next**: ISO-24 (what `AX-11` reads) was blocked on ISO-23, and ISO-23 is now done.
- **Scratch**: the authoring drafts, the raw judge readings and the driver live in this office's
  scratch directory. Nothing committed depends on them. The committed record is the bundles,
  their `gates.json`, the `coverage.json` files and the ledger.
