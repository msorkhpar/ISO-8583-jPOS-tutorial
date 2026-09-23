# ISO-25 handoff: the pilot quiz

**Office** `po-int`. **Branch** `int/m10-pilot-quiz`, from `acde021` (the workspace pin).
**Framework** the corpus pin `3758d114`, exported with `git archive` and never checked out. The
pin carried everything this row needed: the exercises skill and `studyforge.exercise` have no
diff between `3758d114` and `release/m0-foundations` at `15eda744`. `workspace.json` was not
re-pinned; the register re-pins.

⛔ **The built page still carries the key.** The framework at `3758d114` renders
`data-practice-correct` and every option's sentence into the page, as spec §7 §7 described it.
The user's ruling of 2026-09-23 moves the key to the local study server, and that change is
the framework's `W451`. No generated page was hand-edited to hide it. When `W451` merges, the
register re-pins and rebuilds, and this page then stops carrying the key. The bundle's shape
does not change.

## The page, and why

`iso-fundamentals` unit 1, `src/1.md`, *Introduction to ISO-8583*.

- **Its subject is ideas, not code.** It covers what the standard is for (1.1), how its three
  versions differ (1.2) and why it matters for standardization and interoperability (1.3).
- **Its one Java fence is illustration.** The fence is placed after the prose ("To demonstrate
  the practical application…"), and none of the page's claims depend on it.
- **It has no pilot exercise**, and its unit has no source practice, so the quiz is
  `practice-1`.
- The other candidates were weaker. Units 7 and 11 have no jPOS code, but they are still code
  walkthroughs. Unit 16 is a closing chapter with three Java fences.

The ruling is corpus data. The unit's `coverage.json` declares `kind: quiz`. To reverse it,
remove `exercises/iso-fundamentals/prose/unit-01/` and the unit's emitted
`archive/.../unit-01/practice-1.json` (finding F2), restore the container map's count, and run
the pass again.

**Readings:** 475 words (`short`), 2 checkable skills (what the standard is for, and how its
versions differ), tier `introductory`. The plan is 1. ⚠️ A reading of 3 skills would have
planned 2. The count of 2 is this office's judgement, and it agrees with the ask for one quiz.

## The quiz: four questions, each with its own origin

| id | asks | key | origin |
|---|---|---|---|
| `q-2003` | where the page mentions chip cards | c, as something the 2003 version added support for | 1.2.3 |
| `q-1993` | where else, besides 1993, security comes up | b, among what the 2003 version expanded | 1.2.3 |
| `q-regions` | which benefit "faster adoption across regions" is listed under | c, global standardization | 1.3.1 |
| `q-roles` | which option is one of the page's four roles of ISO-8583 | d, structured, efficient transmission of complex transaction data | 1.1.2 |

No origin is 1.3.2 or an enclosing heading, so the quiz accounts for no ledger fence. The one
Java fence carries a written reason.

## The gate record

`exercises/iso-fundamentals/prose/unit-01/practice-1/gates.json`: `Q1`–`Q5` all held, and the
record clears.

- **Q1** was read twice, by two separate model sessions. Each saw the page (prose verbatim, the
  Java fence summarised in one line) and each question's stem and options, with no key and no
  sentences. Both sessions picked the key on all four questions.
- **Q2** was read twice, by two separate sessions that did not see the page and used no tool or
  file. On all four questions, both answered `NONE`.
- **Q3** was read by one session that saw the page and one wrong option at a time. All 11 wrong
  options were refuted, each by a named section and a quoted passage. ⚠️ On `q-roles` option b
  ("seamless communication between banks, processors and networks"), the judge noted that its
  refutation is weaker than the rest, because role 1 in 1.1.2 ("a standardized format for
  communication between various entities") is close in meaning. The option's words are from
  1.3.2's list. A reader who picks it is told why, but it is the quiz's softest distractor.
- **Q4 and Q5** were re-run mechanically after the pass, with `key_total_and_single`, and with
  `origin_still_resolves` against a freshly taken ledger. Both held.

⭐ **The judge seam, exactly as the skill states it.** `judge(brief, questions)` returned one Q1
and one Q2 `Judgement` per question and one Q3 per wrong option, each taken over
`question_digest(question)`. Because the pass is synchronous and the judge is a model
session, the author and the judge were file-backed. The pass paused when a draft or a reading
was missing, wrote nothing, and was re-run once the file existed (`ISO-M10/5`). `held` was
computed from the sessions' raw answers: Q1 is both readings equal the key; Q2 is no reading
equals the key; Q3 is refuted with a section and a quote. The author never judged its own
work. The driver lived in scratch and is not committed.

### The authoring history, stated because the record shows only the last attempt

1. **Pass 1** (three questions, committed at `0c86683`). Attempt 1 was refused by `Q2`: without
   the page, a session picked the key of the 1993 question. Attempt 2 re-authored that question
   under the same id, and it cleared.
2. The browser reading then showed every sentence doubled ("Right. Right. …"): the page already
   prefixes each sentence with *Right.* or *Not this one.* (F5). **Pass 2** re-wrote the
   sentences. Attempt 1 was refused by `Q2` on two questions that had held in pass 1: a fresh
   session picked their keys (F7). Attempt 2 re-authored both and added `q-roles`, and `Q2` was
   taken twice from then on. It cleared, but every key sat at option `a` (F6).
3. **Pass 3** re-ordered the options. Attempt 1 was refused by `Q2` on three questions: a session
   picked the key where it was the longest and most specific option. Attempt 2 made the options
   parallel in length and style, and it cleared. **This is what ships.**

Each re-authoring removed the unit's bundle directory and its emitted archive practice first.

## Proof

- `studyforge validate .` at `3758d114`: GREEN, exit 0 (0 findings, 0 unchecked claims).
- `python3 -m ingest . 2026-09-21` emits `archive/iso-fundamentals/raw/prose/unit-01/practice-1.json`
  and raises unit 1's `practices` to 1. Nothing else in the archive changed.
- `exercises/ledger.json` against `acde021`: additions only (`src/1.md` and its one fence, with
  a written reason). The pilot's 28 entries are byte-identical.
- `studyforge build . --out .`: exit 0, and one page changed
  (`src/study/iso-fundamentals.unit-01-introduction-to-iso-8583.unit.html`).
- The corpus suite (`python3 -m pytest tests`, framework `3758d114`), including the adapter's
  three new quiz tests: GREEN, exit 0, on the committed branch.
- **Browser:** the built branch was served on loopback port 8793 (never 8770) and read in
  headless Chrome over CDP. The page renders *Check yourself*, then a panel headed *Answer
  these*, labelled *Written for this site from this page.*, with four numbered questions, radio
  options and a *Check answers* button. A wrong answer on `q-2003` (option a) with the other
  three right read "3 of 4 answered correctly.", and each question showed its sentence once
  (for example "Not this one. The page's account of 1993 speaks of new and refined data
  elements; chip cards appear only under 2003."). Correcting it read "Every question answered
  correctly.". Grading ran in the page with no request, which is the pre-`W451` behaviour. The
  serve and the browser were stopped afterwards, and `:8770` was not touched.

## Findings, for the framework (none patched here)

- **F1 [structural]: adding a page to an authored corpus is refused at `exercises/ledger.json`.**
  Adding a page changes the ledger's bytes, and `commit` refuses the rewrite (R3). The only way
  through is to remove the ledger, and then every carried reason is lost unless the author
  re-supplies it word for word. This office returned the 13 pilot reasons verbatim from the
  committed ledger. The pass should merge a ledger that only grows.
- **F2 [structural]: re-authoring a unit after emission numbers past its own earlier output.**
  `carried_practices` counts the unit's archived practices, and the archive already holds the
  previous emission of the same authored quiz. So the re-authored bundle landed at `practice-2`.
  The guide's remedy (remove the unit's directory, the ledger and the `practice/` directory)
  leaves out the archive document. Measured on this branch.
- **F3 [R19]: the framework has no `emit` for a quiz.** The guide says the adapter builds the
  document. This corpus wrote the blocks above the questions itself (`QUIZ_BLOCKS` in
  `ingest/exercises.py`), and the next corpus would write them again.
- **F4: `validate` does not re-run `Q5`, and re-runs `Q4` only as the record reader's refusal.**
  The brief and spec §7 §7 say `validate` re-runs both. The authoring guide and
  `validate/exercises.py` say it does not. This office re-ran both by hand. The spec and the
  code disagree.
- **F5: the page prefixes every sentence with *Right.* or *Not this one.*.** The authoring guide
  does not say so. An author who writes the same words gets them twice, and no gate catches it.
- **F6: options render in authored order.** Nothing shuffles them and nothing warns when every
  key sits at the same position. Pass 2 cleared with all four keys at `a`.
- **F7: `Q2` read once is not stable.** The same question drew `NONE` in one session and the key
  in another, across passes. The spec says `Q1` is taken twice and does not say how often `Q2`
  is taken. This office took `Q2` twice and required both readings to miss the key. The
  framework should state `Q2`'s count.
- **F8 (minor):** a quiz page's coverage records `case: code-no-tests`, read off its one fence.
- **F9 (visual):** each question's list number sits beside its first option, not beside the
  stem.
- **Host:** another office deleted the session scratchpad at about 11:33. This office's scratch
  was recreated under a directory named for this row.

## ISO-26: regenerated on framework 79797b90 (W451 merged)

- The re-pin went through `reonboard`. The framework now writes `pin_api` 2 (`"where": "installed"`,
  version `0.1.0`), so the stubs, ONBOARDING.md and the pin test describe an installed library.
  The regenerated execution output is byte-identical at code-server-toolchain `a34a93e`.
- The key grep covers every `src/study/*.html` and `.studyforge/assets/*` file: the
  `data-practice-correct` attribute, each option's sentence (raw and HTML-escaped) and each key
  marker, 35 needles. It reads 24 hits at `e221026` (the positive control) and 0 hits after the
  rebuild.
- The browser run was on port 8794. Right answers read "Every question answered correctly." and
  wrong answers read "2 of 4 answered correctly." Each check sent exactly one
  `POST /api/v1/quiz/iso-8583-jpos-tutorial/iso-fundamentals/unit-01/practice-prose/q-…=…` and
  got 200. F9 (the question numbering) is fixed at this framework.
- ⛔ **Finding F10 (against W451, not patched): the same served origin still serves the key.**
  `serve --site .` serves the corpus root, because the site is built into it (`--out .`). So
  `GET /archive/iso-fundamentals/raw/prose/unit-01/practice-1.json` and
  `GET /exercises/iso-fundamentals/prose/unit-01/practice-1/tests/quiz.json` both answer 200,
  and the first carries `"correct": true` four times. The page no longer holds the key, but a
  reader who opens either URL reads it. Whether `serve` should refuse `archive/` and
  `exercises/`, or the site should be built outside the corpus root, is the framework's call.
