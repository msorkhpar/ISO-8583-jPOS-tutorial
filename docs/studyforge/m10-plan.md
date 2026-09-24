# M10 step 10.4 — the ISO rows

**Corpus backlog, milestone `M10` ("every corpus has practices"), step 10.4.** Companion to
`tasks.md` on `release/studyforge-integration`, whose rows end at `ISO-17`; the rows here are
numbered after it and follow its shape (*Milestone · Depends on · Team · Status*, *Owns*,
*Context*, *Definition*, *Acceptance*).

**Taken at:** this corpus at `06df27f` (the commit the framework's `workspace.json` pins), the
framework release tip `5e0657ba` (PO round 147), `code-server-toolchain` at `4b3fcdb` (the pinned
sibling). Office: `po-int`. Branch `int/m10-step-10.4-plan`.

⛔ **This document authors no exercise material.** Spec §9: the authoring skill and its guide
(framework step 10.3) land before the artifact they produce. `AX-08` is merged (`e01c7243`);
`AX-10`, the guide, is still in flight, so no row here that writes material can start yet.

⛔ **Standing rules, inherited from `tasks.md` unchanged:** no row modifies `studyforge` (a
shortfall is a finding, below); everything is additive (R3), and `permitted_edits` stays `[]`;
acceptance is green or red; a hand-edit to a generated artifact is a finding (R19); no absolute
path and no personal data anywhere (R7).

**Repository shorthand**, relative to the workspace root (R18): `SF/` = `studyforge/`,
`ISO/` = this repository, `CST/` = `code-server-toolchain/`.

---

## 1. The census, measured at `06df27f`

### 1.1 How it was counted

⭐ **With the framework's own readers, not a second grammar**: `scan` (the fence walk the source
ledger uses), `words_of` (the one spelling of a page's words, fences left out) and `band_for`
(the plan's length band), all from `SF/src/studyforge/skills/exercises/` at `5e0657ba`. So the
fences counted here are the ledger's entries, one for one. Run from this repository's root:

```sh
SF=$(mktemp -d) && git -C ../studyforge archive 5e0657ba src | tar -x -C "$SF"
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH="$SF/src" python3 - <<'EOF'
import json, pathlib
from studyforge.skills.exercises.scan import scan
from studyforge.skills.exercises.drafts import words_of
from studyforge.skills.exercises.plan import band_for
root = pathlib.Path(".")
for c in ("iso-fundamentals", "jpos-client", "jpos-server"):
    for u in json.loads((root / "archive" / c / "container.json").read_text())["units"]:
        text = (root / u["origin"]).read_text(encoding="utf-8")
        fences = scan(text).fences
        java = [f for f in fences if (f.language or "").lower() == "java"]
        lines = sum(1 for f in java for l in f.body.splitlines() if l.strip())
        jpos = sum(1 for f in java if "org.jpos" in f.body)
        junit = sum(1 for f in fences if "org.junit" in f.body)
        ex = {}
        if u.get("practices"):
            doc = root / "archive" / c / "raw/prose" / f"unit-{u['n']:02d}" / "practice-1.json"
            ex = json.loads(doc.read_text())["exercise"]
        tests = ex.get("test_path", "-")
        case = "code+tests" if "test_path" in ex else ("code, no tests" if java else "neither")
        print(c, u["n"], u["origin"], words_of(text), band_for(words_of(text)).name,
              len(fences), len(java), lines, jpos, junit, tests, case, sep="\t")
EOF
```

`../studyforge` is the sibling checkout from the corpus's main checkout; from a linked worktree it
is one level further up. ⛔ **The practice files (`src/p1.md`–`p3.md`) carry no fence at all** —
their starting code lives in `practice/*/` — so each unit's row is its lesson page alone.

**Test files**: `git ls-files 'practice/**/*Test.java'` prints two, and the archive's practice
documents name the same two as `test_path` (`archive/iso-fundamentals/raw/prose/unit-0{2,4}/practice-1.json`).
⚠️ **Neither is the tutorial's own.** Both were added at `cd84017` by the framework's `W426` (M7's
three sample practices), and both are plain-Java `main` programs, not JUnit. The tutorial itself
ships no test file.

### 1.2 Per container

| container | units | Java fences | Java lines (non-blank) | fences importing `org.jpos` | fences importing `org.junit` | test files | code + tests | code, no tests | neither |
|---|---|---|---|---|---|---|---|---|---|
| `iso-fundamentals` | 16 | 76 | 2003 | 47 | 11 | 2 | 2 (units 2, 4) | 14 | 0 |
| `jpos-client` | 11 | 59 | 1443 | 51 | 12 | 0 | 0 | 11 | 0 |
| `jpos-server` | 11 | 53 | 1614 | 43 | 11 | 0 | 0 | 11 | 0 |
| **total** | **38** | **188** | **5060** | **141** | **34** | **2** | **2** | **36** | **0** |

⭐ **Every unit carries at least one Java fence, so no ISO page is `neither` and none needs the
quiz shape.** All fences across the 38 lesson pages: 218 — `java` 188, `xml` 23, unlabelled 5,
`json` 1, `properties` 1. ⭐ **36 of 38 units have at least one fence importing `org.jpos`**
(the exceptions are `iso-fundamentals` units 7 and 11), which is why `W390`'s prime is on the
critical path of every row below that runs a gate.

⛔ **The length bands no longer plan anything (framework `W453`, the user's ruling of
2026-09-23; finding `W453/3`).** At `06df27f` this section read `AX-07`'s `BANDS`, which measure
prose only: 34 units `short` (ceiling 2), one `standard`, and three `stub` that planned zero
(`iso-fundamentals` unit 11, `jpos-client` unit 5, `jpos-server` unit 5) though they carry 7, 5 and
5 Java fences. Under `W453` a page is planned by its important ideas, prose and code. There is no
ceiling and no quota. Each of those three units now ships one exercise. `ISO-M10/6` is withdrawn
by the same ruling.

### 1.3 Per unit

`words` is `words_of`. It is kept as a reading, and it sets nothing now (`W453/4`). `java`,
`lines`, `jpos` and `junit` are as in §1.1. ⭐ **`ISO-23` replaces the `band` column** that stood
here. It gives what the `W453` pass shipped on each unit: `N code`, `N quiz`, `0 by plan` (every
aspect carries a written reason), or `0` with the gate that refused. On `iso-fundamentals` units
2, 3 and 4 the count leaves out the source's own `practice-1`, which the unit carries as well.

| container | unit | page | words | ISO-23 | fences | java | lines | jpos | junit | case |
|---|---|---|---|---|---|---|---|---|---|---|
| iso-fundamentals | 1 | `src/1.md` | 475 | 1 quiz | 1 | 1 | 34 | 1 | 0 | code, no tests |
| iso-fundamentals | 2 | `src/2.md` | 454 | 1 code | 3 | 3 | 48 | 3 | 0 | code + tests (`practice/bitmap/BitmapTest.java`) |
| iso-fundamentals | 3 | `src/3.md` | 768 | 1 code | 5 | 3 | 60 | 2 | 0 | code, no tests (its practice has no grader) |
| iso-fundamentals | 4 | `src/4.md` | 323 | 2 code | 9 | 9 | 188 | 9 | 0 | code + tests (`practice/mti/MtiTest.java`) |
| iso-fundamentals | 5 | `src/5.md` | 503 | 1 code | 3 | 2 | 109 | 2 | 1 | code, no tests |
| iso-fundamentals | 6 | `src/6.md` | 273 | 1 code | 4 | 4 | 160 | 3 | 1 | code, no tests |
| iso-fundamentals | 7 | `src/7.md` | 353 | 1 code | 4 | 4 | 123 | 0 | 0 | code, no tests |
| iso-fundamentals | 8 | `src/8.md` | 446 | 1 code | 4 | 4 | 141 | 4 | 0 | code, no tests |
| iso-fundamentals | 9 | `src/9.md` | 364 | 1 code | 2 | 2 | 124 | 2 | 1 | code, no tests |
| iso-fundamentals | 10 | `src/10.md` | 569 | 1 code | 7 | 7 | 206 | 6 | 6 | code, no tests |
| iso-fundamentals | 11 | `src/11.md` | 197 | 1 code | 8 | 7 | 120 | 0 | 0 | code, no tests |
| iso-fundamentals | 12 | `src/12.md` | 331 | 1 code | 5 | 5 | 137 | 2 | 1 | code, no tests |
| iso-fundamentals | 13 | `src/13.md` | 578 | 1 code | 7 | 6 | 104 | 1 | 1 | code, no tests |
| iso-fundamentals | 14 | `src/14.md` | 419 | 2 code | 6 | 6 | 166 | 5 | 0 | code, no tests |
| iso-fundamentals | **15** | `src/15.md` | 456 | 2 code | 10 | **10** | 206 | 4 | 0 | code, no tests |
| iso-fundamentals | 16 | `src/16.md` | 677 | 1 quiz | 5 | 3 | 77 | 3 | 0 | code, no tests |
| jpos-client | 1 | `src/c1.md` | 312 | 0 by plan (`ISO-M10/12` ruling) | 9 | 4 | 62 | 4 | 1 | code, no tests |
| jpos-client | 2 | `src/c2.md` | 306 | 1 code | 5 | 5 | 125 | 4 | 1 | code, no tests |
| jpos-client | 3 | `src/c3.md` | 290 | 1 code | 7 | 7 | 150 | 6 | 0 | code, no tests |
| jpos-client | 4 | `src/c4.md` | 278 | 1 code | 6 | 5 | 113 | 5 | 1 | code, no tests |
| jpos-client | 5 | `src/c5.md` | 180 | 1 code | 5 | 5 | 201 | 5 | 0 | code, no tests |
| jpos-client | 6 | `src/c6.md` | 328 | 2 code | 7 | 7 | 124 | 7 | 0 | code, no tests |
| jpos-client | 7 | `src/c7.md` | 318 | 1 code | 4 | 3 | 116 | 3 | 1 | code, no tests |
| jpos-client | **8** | `src/c8.md` | 435 | 2 code | 8 | **8** | 119 | 8 | 1 | code, no tests |
| jpos-client | 9 | `src/c9.md` | 328 | 2 code | 7 | 7 | 122 | 1 | 1 | code, no tests |
| jpos-client | 10 | `src/c10.md` | 331 | 1 code | 5 | 5 | 177 | 5 | 5 | code, no tests |
| jpos-client | 11 | `src/c11.md` | 412 | 0 by plan | 4 | 3 | 134 | 3 | 1 | code, no tests |
| jpos-server | 1 | `src/s1.md` | 277 | 0 by plan | 7 | 3 | 48 | 3 | 1 | code, no tests |
| jpos-server | 2 | `src/s2.md` | 267 | 1 code | 5 | 4 | 147 | 4 | 0 | code, no tests |
| jpos-server | 3 | `src/s3.md` | 321 | 1 code | 4 | 3 | 106 | 3 | 0 | code, no tests |
| jpos-server | 4 | `src/s4.md` | 338 | 1 code | 5 | 4 | 150 | 3 | 1 | code, no tests |
| jpos-server | 5 | `src/s5.md` | 214 | 1 code | 6 | 5 | 202 | 5 | 1 | code, no tests |
| jpos-server | 6 | `src/s6.md` | 366 | 1 code | 4 | 3 | 91 | 3 | 0 | code, no tests |
| jpos-server | 7 | `src/s7.md` | 259 | 1 code | 6 | 6 | 130 | 6 | 1 | code, no tests |
| jpos-server | **8** | `src/s8.md` | 340 | 2 code | 10 | **9** | 125 | 6 | 1 | code, no tests |
| jpos-server | 9 | `src/s9.md` | 260 | 1 code | 8 | 5 | 194 | 1 | 1 | code, no tests |
| jpos-server | 10 | `src/s10.md` | 263 | 1 code | 6 | 4 | 106 | 2 | 0 | code, no tests |
| jpos-server | 11 | `src/s11.md` | 366 | 1 code | 7 | 7 | 315 | 7 | 5 | code, no tests |

---

## 2. The pilot: three pages, and the density measure that chose them

⭐ **The measure: Java fences on the unit's lesson page — its Java ledger entries — ties broken by
non-blank Java lines, among pages whose band is not `stub`.**

- **Why fences and not lines.** Each fence is one ledger entry that must either be the basis of an
  exercise or carry a written reason (property 1, *nothing is lost*). The pilot is where the user
  approves the **shape and the per-page counts**, and the count's hardest test is the page with
  the most entries to account for against a ceiling of 2. A 300-line fence is one entry; ten short
  fences are ten.
- **Why not a `stub` page.** A `stub` page plans zero by construction (`AX-07`), so it would show
  the user no exercise at all. That is a legitimate answer for such a page, and `ISO-23` reads it,
  but it cannot be the page on which the shape is approved.

| container | pilot page | Java fences | runner-up by the same measure | measured by lines instead |
|---|---|---|---|---|
| `iso-fundamentals` | **unit 15, `src/15.md`** (456 words, `short`) | **10** (206 lines, 4 on jPOS) | unit 4: 9 | unit 15 (206, tie with unit 10, broken by fences) |
| `jpos-client` | **unit 8, `src/c8.md`** (435 words, `short`) | **8** (119 lines, all 8 on jPOS, 1 JUnit) | units 3, 6, 9: 7 each | unit 5 is `stub`, so unit 10 (177) |
| `jpos-server` | **unit 8, `src/s8.md`** (340 words, `short`) | **9** (125 lines, 6 on jPOS, 1 JUnit) | unit 11: 7 | unit 5 is `stub`, so unit 11 (315) |

⭐ **Each maximum is unique**, so no tie-break decided a pilot page. ⚠️ **The two measures
disagree on two of the three containers**, which is why the choice is argued above, not assumed.
⚠️ **Not one pilot page is case (a).** The only case-(a) pages are `iso-fundamentals` units 2 and
4, and unit 4 is one fence behind unit 15. The user would then never review a code-with-tests
page. That is a question for the PO (`ISO-M10/7`), not a substitution this office makes quietly.

---

## 3. The rows

### ISO-18 — Re-pin to the authoring framework, and regenerate what the pin owns
**Milestone** M10 · 10.4 · **Depends on** framework `AX-08` (`e01c7243`, in `5e0657ba`); `AX-03`
`9ed320c9`, `AX-04` `638ec209`, `AX-06` `32391172`, `AX-07` `d7b80ed2`, `AX-09` `d459fadc` ·
**Team** solo · **Status** ready
**Owns** `.studyforge/pin.json` and the three `.studyforge/skills/*.md` stubs, **regenerated,
never hand-edited**; `tests/test_framework_pin.py` as the onboarding skill regenerates it
**Context** ~20k — `SF/src/studyforge/skills/onboarding/`, this document's `ISO-M10/1`

**Definition.** The corpus records framework `638e233` (W426). That commit predates every `AX` row,
and it does not read this corpus's own archive (`ISO-M10/1`). Move the pin in one step, by the
onboarding skill, to a release commit that carries `AX-08`. ⚠️ **If `AX-10` has merged by then,
pin past its merge.** Otherwise `ISO-22` re-pins before it authors (§9).

**Acceptance.**
- `.studyforge/pin.json`'s commit `C` satisfies `git -C ../studyforge merge-base --is-ancestor e01c7243 C`
  (exit 0). A planted pin at `638e233` fails the same check (exit 1).
- The corpus's own suite is GREEN at `C` (`PYTHONPATH=<SF at C>/src:. python3 -m pytest tests`),
  including `tests/test_framework_pin.py::test_no_stub_has_drifted_from_the_pin`, which is **RED
  at `06df27f`**.
- `studyforge validate .` at `C` is GREEN.
- `git diff 06df27f -- src README.md TestCases.md LICENSE .gitignore practice archive` is empty (R3):
  the re-pin moves no material and no emitted document.

### ISO-19 — The manifest classifies the exercise trees, and a run's report is ignored
**Milestone** M10 · 10.4 · **Depends on** ISO-18; `AX-04/2`, `AX-04/3`, `AX-08/4` (the
preconditions `SF/src/studyforge/skills/exercises/SKILL.md` §*Before you start* states) ·
**Team** solo · **Status** blocked on ISO-18
**Owns** `corpus.json`'s `content.not_material` (additions only); a **new** file
`practice/.gitignore`
**Context** ~15k — `SKILL.md`, `AX-04`'s handoff §§ *the shape* and *AX-03/1 is closed*, `W435`'s
self-ignore precedent

**Definition.** `AX-08/4`'s preconditions, typed by hand because no skill writes them yet (that gap
is `ISO-M10/4`). Measured against `06df27f`: `corpus_api` is already 4 (2 is required), and
`practice/**` is **already** declared (W426). Two lines are missing:

| glob | why it is needed |
|---|---|
| `exercises/**` | the bundle tree; without it `validate` refuses every bundle file as unclassified before any exercise check runs (`AX-04/2`) |
| `docs/**` | this backlog's own directory; this document is the first file under it on this line, and `validate` reads it as unclassified (`ISO-M10/10`) |

⭐ **The ignore rule is a NEW file, never an edit to the root `.gitignore`** (R3, `permitted_edits:
[]`; `W435`'s remedy is *"write new ignore files inside generated directories"*). It names the
report path and the build output the runtime writes into a reader's workspace. The pattern depends
on the report path the exercises declare (`ISO-M10/4`), so it is written after `ISO-20` fixes the
runtime, and it is taken from that runtime's declared report path, never guessed.

**Acceptance.**
- `studyforge validate .` at the pin is GREEN, with a planted bundle-shaped file under `exercises/`.
  With the `exercises/**` line removed, the same plant is reported `[unclassified]` (both ways, R12).
- A JUnit report planted at the declared report path inside a workspace directory
  `practice/<container>/prose/unit-NN/practice-M/` gives `git check-ignore` exit 0 and
  `git status --porcelain` empty. Without `practice/.gitignore`, the same plant shows as untracked.
- `git diff 06df27f -- .gitignore` is empty, and `corpus.json`'s diff is additions inside
  `not_material` only.

### ISO-20 — The runtime set, and the jPOS prime the runner is built with
**Milestone** M10 · 10.4 · **Depends on** ISO-18; `W390` (`SF` `bb27e808`; `CST` `748f4fb`, an
ancestor of the pinned `4b3fcdb`); `W390/4`; **`ISO-M10/2` answered** · **Team** solo ·
**Status** blocked — `ISO-M10/2` and `ISO-M10/8`
**Owns** `corpus.json`'s `runtimes`; the corpus's practice build file (a new file); what the
execution skill writes under `.studyforge/execution/`; the recorded runner tag
**Context** ~30k — `SF/src/studyforge/skills/execution/SKILL.md`, `W390`'s handoff *For dependents*,
`CST/consuming.json` § `prime`

**Definition.** A graded run is `--network none`, and 141 of the 188 Java fences import `org.jpos`.
The runner must therefore carry jPOS, and the JUnit that writes `AX-01`'s channel, warmed from this
corpus's own build file. The material's own `pom.xml` fences (`src/c1.md`, `src/c7.md`,
`src/s1.md`) all name `org.jpos:jpos:2.1.7`. ⚠️ No fence names a JUnit version (`ISO-M10/8`).
Declare the runtime set (`java` plus the build tool the build file needs, from `RUNTIMES`), run the
execution skill so `prime.prime_for` builds the prime from the corpus's own files, and build the
runner with `--prime`.

**Acceptance.**
- `studyforge validate .` GREEN; `parse_runtimes` accepts the set. `prime.prime_for(root, runtimes)`
  does not refuse, and with the build file removed it refuses by name (R12).
- The runner tag is the one the contract's `tag_from` argv prints. It is recorded beside the `CST`
  commit `workspace.json` pins, and never typed.
- In that runner, started `--network none` with an arbitrary `--user`, the prime's own test passes.
  In the unprimed runner the same command fails (both ways, as `W390`'s reading was taken).
- The three M7 practices still Run and Submit exactly as before, in the new runner: their archived
  `test_command`s give the verdicts `src/Practice.md` says they ship with (the MTI grader passes,
  the bitmap grader fails until fixed). The M7 reading is taken in the runner with this row's tag;
  this host has no `java`.

### ISO-21 — The adapter emits authored bundles beside the three bundled practices
**Milestone** M10 · 10.4 · **Depends on** ISO-19; `AX-04` `638ec209` (`emit`, `emit_page`,
`validate`'s gate-record arm); **`ISO-M10/3` answered** · **Team** solo · **Status** blocked —
`ISO-M10/3`
**Owns** `ingest/practices.py` (and a new module beside it if R11 needs one), `tests/ingest/`
**Context** ~25k — `SF/src/studyforge/exercise/bundle/`, `AX-04`'s handoff, `ingest/`

**Definition.** R2: the adapter reads each committed bundle under `exercises/` and writes its
`practice-M.json` with the framework's `emit`. It carries no corpus-specific code in the framework
and no path arithmetic of its own (`Places` answers every path). ⚠️ `iso-fundamentals` units 2, 3
and 4 already carry a bundled `practice-1`, and the authoring loop numbers from 1 (`ISO-M10/3`).
This row does not ship until that is answered.

**Acceptance.**
- Proved on a **test fixture bundle** under `tests/ingest/` (not corpus material): the emitted
  document round-trips through the archive's reader, and `validate` is GREEN.
- A fixture bundle whose digest drifted is refused by `validate`, naming the file.
- On a unit that already has `practice-1`, the authored exercise takes an ordinal that leaves
  `archive/iso-fundamentals/raw/prose/unit-0{2,3,4}/practice-1.json` **byte-unchanged**
  (`git diff` empty), and the unit's ordinals run `1..n` with no gap.
- With no bundle committed, the archive `ingest` emits is byte-identical to `06df27f`'s (R10).

### ⛔ The standing rule for ISO-22 and ISO-23: what a graded run can rely on

*Added after ISO-20, on the register's ruling on finding 4, which is reversible and is corpus data.*

- ⭐ **The corpus build is the whole dependency set.** `java-build/pom.xml` declares exactly the
  libraries the material's own `pom.xml` fences name, at the versions they name: `org.jpos:jpos`
  2.1.7, `spring-context` 5.3.20, `spring-boot-starter` 2.6.3 and `commons-pool2` 2.11.1. It also
  declares JUnit Jupiter 5.14.4 and Mockito 5.23.0, which the material imports but gives no
  version for. Every exercise's build role names these versions and no others, and the runner
  recorded in `.studyforge/execution/runner.env` is warmed from this build. A library outside this set
  fails `G1` offline. The remedy is this build and a rebuilt runner, never the exercise.
- ⛔ **A fence that needs a live database, or anything else off the machine, gets a written
  reason in the ledger at authoring time. It never becomes an exercise.** A graded run is
  `--network none` with no database behind it. So the PostgreSQL driver the material's fences
  name is deliberately **not** in the corpus build, and no exercise may rely on a JDBC connection,
  a socket to a peer, or a service a container would have to start. The same fence's logic may
  still be the basis of an exercise once the live dependency is removed, for example by a mock
  from the declared Mockito. What may not happen is an exercise whose main ask needs the
  database.

### ISO-22 — The pilot: three pages, one per container, reviewed once by the user
**Milestone** M10 · 10.4 · **Depends on** ISO-19, ISO-20, ISO-21; `AX-08` `e01c7243`; **`AX-10`
merged** (§9; not merged at `5e0657ba`); `AX-09` `d459fadc`; **`ISO-M10/5` answered** ·
**Team** solo, then **the user** · **Status** blocked — `AX-10`, and every blocker above
**Owns** `exercises/iso-fundamentals/prose/unit-15/`, `exercises/jpos-client/prose/unit-08/`,
`exercises/jpos-server/prose/unit-08/`, the matching `practice/…` workspaces, `exercises/ledger.json`,
and the rebuilt site
**Context** ~40k — `SKILL.md`, `AX-10`'s guide, this document §§1–2

**Definition.** Run `author_corpus` over the three pages §2 names: `src/15.md`, `src/c8.md` and
`src/s8.md`. Material is the manifest's `content` policy, graders are none (all three are case
*code, no tests*), and the runner is the primed runner from `ISO-20`. Each `Page` records its three
readings (`words` by `words_of`, `skills`, `tier`), and each planned exercise is authored and gated
inside `ATTEMPTS`. Emit (`ISO-21`), rebuild, and put the three pages in front of the user **once**.
⛔ **The user's review approves the shape and the per-page counts, and nothing after it is
reviewed** (user ruling, 2026-09-19).

**Acceptance.**
- Each pilot unit's `coverage.json` exists: shipped plus refused equals its plan, and every refusal
  names its gate, the gate's sentence and the run's last output.
- Every Java fence of the three pages (10 + 8 + 9 = 27) appears in `exercises/ledger.json`, either
  as an exercise's `origin` or with a written reason.
- `studyforge validate .` GREEN, and every shipped bundle's gate record verifies against the files
  beside it.
- Re-running the pass with nothing changed writes nothing (every file keeps its bytes and mtime).
- ⛔ `git diff 06df27f -- src README.md TestCases.md LICENSE .gitignore` is empty, and the reading
  floor of every non-pilot page is byte-unchanged over `file://`.
- ⭐ **The user's verdict is recorded with the ref it was given at.** It is either *approved* or a
  list of changes. A change re-runs this row (remove the unit's directory and
  `exercises/ledger.json`, per `AX-08`'s *For dependents*). It never opens a second review of
  `ISO-23`.

### ISO-23 — All 38 units, planned by their important ideas (`W453`), on the gates alone
**Milestone** M10 · 10.4 · **Depends on** ISO-22 and ISO-25 (the user approved the pilot);
framework `W453` (pin `6d0b8dc6`) · **Team** solo · **Status** done on `int/m10-iso-23`, handed
back to the register; not merged
**Owns** `exercises/**` and `practice/**` for all 38 units (the pilot's included); `exercises/ledger.json`;
the emitted archive; the rebuilt site; the fixtures in `tests/ingest/`
**Context** one batch per container; handoff `docs/studyforge/handoffs/ISO-23.md`

**Definition.** ⭐ **Re-planned by the user's ruling of 2026-09-23, now framework `W453`** (finding
`W453/3` retires the band reading that stood here). The user's words: *"Depending on the context
of the page there might be no practice, 2 or more, The target is covering all the aspects not just
having something minimum we are looking for quality"* and *"don't over do it! … Sometimes a single
practice might cover better than 4 unrelated small practices."* So each page is planned by its
important ideas (aspects), prose and code. Related ideas share one exercise. An idea not practised
carries a written reason, and a page may plan zero. There is no band, no ceiling and no quota. The
plan_api 1 data (the pilot's three units, the unit-1 quiz, their workspaces and the ledger) is
cleared, and all 38 units are authored again under that rule. A pilot exercise that still fits its
page is carried byte-identical. ⛔ **No further review**: the gates are the only bar. Code must
clear G1–G5 (the starter fails, the reference passes, each plant is caught), and a quiz Q1–Q5.
Q1–Q3 are taken by independent model sessions, never the author: Q1 twice with the page, Q2 twice
without it, Q3 once per wrong option. Whatever does not clear becomes a shortfall that names its
gate. The two case-(a) units (`iso-fundamentals` 2 and 4) hand `author_corpus` their declared
grader.

**Acceptance.**
- Every one of the 38 units has a `coverage.json`. Shipped plus refused equals the plan on each, and
  a page with a zero plan says why.
- `exercises/ledger.json` accounts for all 218 fences of the 38 lesson pages and both declared test
  files, each by an exercise's `origin` or a written reason. An entry with neither is refused
  (`account`).
- `studyforge validate .` GREEN, and the corpus suite GREEN. The three bundled M7 practices are
  byte-unchanged in `practice/` and in the archive.
- The site is rebuilt. A page changes only where its practices changed, and a real browser over a
  served origin grades one code practice and one quiz.

**Result** (at `a3936b6`, the unit-1 zero plan at `dda566b`; framework `6d0b8dc6`; runner
`code-server-toolchain/runner:java-maven-amd64-54f18c498a95`, offline).

| container | units | code | quiz | zero by plan | shortfall |
|---|---|---|---|---|---|
| `iso-fundamentals` | 16 | 17 | 2 (units 1, 16) | 0 | 0 |
| `jpos-client` | 11 | 12 | 0 | 2 (units 1, 11) | 0 |
| `jpos-server` | 11 | 11 | 0 | 1 (unit 1) | 0 |
| **total** | **38** | **40** | **2** | **3** | **0** |

- 42 exercises shipped. Per unit, §1.3's `ISO-23` column. Most units have one exercise. Seven have
  two (`iso-fundamentals` 4, 14 and 15; `jpos-client` 6, 8 and 9; `jpos-server` 8). The three
  units the bands planned at zero each ship one.
- ⭐ **`jpos-client` unit 1 is a zero plan, by the register's ruling on `ISO-M10/12`**
  (`dda566b`; reversible, as corpus data). Its quiz was refused by `Q2` on all three attempts:
  the page's ideas, the crossing send and receive queues and finding the mux by name, are standard
  jPOS convention that general knowledge answers without the page. Every aspect and fence carries
  that reason, and the corpus has no shortfall.
- 38 `coverage.json` files. The ledger holds 220 entries (218 fences and 2 graders). `validate`
  GREEN, exit 0; the suite GREEN, exit 0. The M7 practice trees (`practice/bitmap`, `fields`,
  `mti`) and the archived `practice-1` of `iso-fundamentals` units 2, 3 and 4 are byte-unchanged
  against `424e55a`.
- Site: 32 of the 38 unit pages changed. The 6 unchanged pages are the three pilot pages
  (`iso-fundamentals` 15, `jpos-client` 8, `jpos-server` 8), and the three pages that ship nothing.
  Against `424e55a`, the pilot's bundles differ only in each unit's `coverage.json` (the `W453`
  plan) and in `iso-fundamentals` unit 15 practice-2's `pom.xml` and the gate record over it. The
  pom is not on the page.
- Browser (headless Chrome, served on a spare port):
  - Quiz, `iso-fundamentals` unit 1: answered one right and one wrong. The page POSTed
    `/api/v1/quiz/…` and showed "1 of 2 answered correctly".
  - Code, `iso-fundamentals` unit 5: Submit on the starter showed main ask not yet and edge cases
    0 of 2. With the reference in place: main ask done, edge cases 2 of 2.
  - ⚠️ Code was graded in host mode, through a scratch `mvn` shim that runs the pinned runner
    image offline. See `ISO-M10/13`.

### ISO-27 — The tutorial's own page defects, fixed (the user's ruling: "fix them")
**Milestone** M10 · 10.4 · **Depends on** ISO-23 · **Team** solo · **Status** done on
`int/m10-iso-23`, handed back to the register; not merged. Handoff
`docs/studyforge/handoffs/ISO-27.md`

The defects ISO-23's authors found on 16 source pages (`src/6`, `7`, `8`, `11`, `12`, `13`;
`c1`–`c4`; `s2`–`s7`) are fixed minimally.
- Headings and fence ordinals are unchanged.
- Every changed fence is proven against `java-build/pom.xml`, offline.
- The 15 exercises on those pages are re-gated: 2 re-authored, 4 with the statement only, and the
  rest byte-identical.
- By the user's second ruling, `spring-web` 5.3.20 is declared, and the runner is rebuilt and
  recorded as `…-7a5a2f2aacba`.

### ISO-28 — The second "fix them" pass, headings included
**Milestone** M10 · 10.4 · **Depends on** ISO-27; framework `W456` (pin `de1ea776`) · **Team**
solo · **Status** done on `int/m10-iso-23`, handed back to the register; not merged. Handoff
`docs/studyforge/handoffs/ISO-28.md`

Every defect ISO-27 recorded but did not edit is fixed across 12 pages, and 6 wrong-claim headings
are renamed (c3 ×5, s3 ×1). `src/6.md`'s "Void 02" and "cash advance 01" are left unconfirmed.
The corpus is re-pinned to `de1ea776`, and the pass ran at that pin with the ledger merged in
place: nothing was dropped, and only the fixed pages' rows changed. jpos-server unit 3's origin
follows its renamed heading. validate (check 20 included) is GREEN, and so is the suite.

### ISO-24 — What `AX-11` reads here, named and dry-read before it runs
**Milestone** M10 · 10.4 → 10.5 · **Depends on** ISO-23; framework `AX-11` (not started) ·
**Team** solo · **Status** blocked on ISO-23
**Owns** nothing. ⛔ This row produces evidence, like `AX-11` itself: a row that could edit what it
measures measures nothing.
**Context** ~15k — `E14` § AX-11, `AX-08`'s handoff *For `AX-11`*

**Definition.** Hand `AX-11` a corpus where each of its conditions has a named file or command,
and take a dry reading of each before `AX-11` does, so a shortfall shows up in this corpus's
findings first rather than in the milestone's closing reading.

| `AX-11` reads on ISO | where it reads it here |
|---|---|
| every page has a plan, and ships its planned exercises or names the gate that refused each | the 38 `exercises/<container>/prose/unit-NN/coverage.json`, read with no run. A zero plan (three units) is a legitimate answer; a page in `Authored.bare` with a non-zero plan is a shortfall |
| every shipped exercise's gate record verifies against the files beside it | per bundle, `gates.drifted(root / bundle, record.inputs, bundle) == ()`, and `studyforge validate .` |
| on a planted partial solution, Submit shows *main ask ✓* with the missed edge cases named | a pilot exercise's `plants/edge-N/` copied over its workspace file, then Submit through the served corpus (`AX-02`, `AX-09`) |
| the ledger accounts for every Java fence in the corpus | `exercises/ledger.json` against §1.2's 188 Java fences, re-counted at the reading's ref by §1.1's command |
| the reading floor is byte-unchanged over `file://` | the built lesson pages against `ISO-22`'s pre-pass build, diffed |

**Acceptance.** Each line above was read at a recorded ref and environment, with GREEN/RED and the
exit code. A RED is a finding against the row that owns the surface, never an edit from this row.

### ISO-25 — The pilot quiz: one conceptual page, declared `quiz`
**Milestone** M10 · 10.4 · **Depends on** ISO-21 (the adapter), ISO-22 (the pilot's bundles and
ledger); framework `AX-05`, `AX-06`, `AX-08` (all in the pin `3758d114`); ⛔ **framework `W451`**
(the key leaves the page; in flight) · **Team** solo · **Status** authored, emitted and built on
`int/m10-pilot-quiz`; ⛔ **the built page carries the key until `W451` merges and the register
re-pins and rebuilds**
**Owns** `exercises/iso-fundamentals/prose/unit-01/` (the quiz bundle and its coverage report);
`exercises/ledger.json` (additions only); the quiz arm of `ingest/exercises.py` and its tests;
`archive/iso-fundamentals/raw/prose/unit-01/practice-1.json`; the rebuilt unit-1 page
**Context** ~25k — `SF/src/studyforge/skills/exercises/SKILL.md`, spec §7 §7, `W451`'s row;
handoff `docs/studyforge/handoffs/ISO-25.md`

**Definition.** §1.2 found no ISO page `neither`, so the ledger planned no quiz. The user wants
one multiple-choice quiz in the pilot, beside the six code exercises. ⭐ **The register rules,
reversibly and as corpus data, that one conceptual page is a `quiz` page.** This office chose
`iso-fundamentals` unit 1, `src/1.md` (*Introduction to ISO-8583*). Its subject is what the
standard is, how its versions differ and why it matters. Its one Java fence is an illustration
placed after the prose, and the unit has no pilot exercise. The unit's coverage report declares
`kind: quiz`. To reverse the ruling, remove that unit's directory, its emitted practice and the
ledger, then run the pass again. The readings are 475 words (`short`), 2 checkable skills (what
the standard is for, and how its versions differ) and tier `introductory`, so the plan is **1**:
one quiz of 3 to 5 questions.

⛔ **The quiz is authored against the CURRENT bundle shape**, with the key in `tests/quiz.json`
as `AX-05` writes it. The user's ruling of 2026-09-23 moves the key to the local study server
(`W451`). No generated page is hand-edited to hide it. That work is `W451`'s, in the framework.

**Acceptance.**
- `exercises/iso-fundamentals/prose/unit-01/coverage.json` exists: kind `quiz`, plan 1, shipped
  1, no shortfall.
- The quiz's gate record holds `Q1`–`Q5`. `Q1`–`Q3` were taken over each question's digest by
  independent model sessions, never by the author. `Q4` and `Q5` are re-run mechanically, and
  both hold.
- The diff of `exercises/ledger.json` against `acde021` is additions only: the pilot's 28 entries
  are byte-unchanged, and the one fence in `src/1.md` carries a written reason.
- `python3 -m ingest . 2026-09-21` emits the quiz as `practice-1` on unit 1 and raises the unit's
  count to 1. Nothing else in the archive changes, and `studyforge validate .` is GREEN.
- The rebuilt site changes one page, unit 1. A real browser over a served origin reads the quiz
  and grades it.
- ⛔ **Open until `W451` merges and the register re-pins and rebuilds:** no built page carries
  the quiz's key. At the pin `3758d114`, the page still carries it.

---

## 4. Blockers and questions for the framework — findings, not patches

⛔ **None of these is fixed in this diff, and none may be fixed in `studyforge` by this office.**

| # | triage | blocks | what |
|---|---|---|---|
| `ISO-M10/1` | `[local]`, baseline | ISO-18 | ⛔ **At `06df27f` the corpus's own suite is RED at every framework ref tried.** At its recorded pin `638e233`: exit 1, 15 failed, and `studyforge validate .` is RED because it refuses `container_api 3` in all three container maps. **The recorded pin cannot read its own archive.** At `5e0657ba`: exit 1, one failure, `test_no_stub_has_drifted_from_the_pin`: the three stubs name `aa4e256` and `pin.json` names `638e233`. The pin was advanced at `bc08aaa` without regenerating the stubs. `validate` is GREEN at `5e0657ba` (exit 0). ⭐ `ISO-18` is the remedy. The framework question is whether a pin advance that skips the stubs should be refusable at commit time |
| `ISO-M10/2` | `[structural]` | ISO-20, ISO-22, ISO-23 — every jPOS exercise | ⛔ **No framework shape carries a Java exercise's third-party classpath.** A bundle's file set is closed (`bundle.json`, `statement.md`, `gates.json`, `starter/`, `reference/`, `tests/`, `plants/`). `emit` writes only `main_file` and `test_file` into the workspace, and refuses a command argument outside the workspace. So no build file can sit in the workspace, and none outside it can be named. `AX-01`'s channel is JUnit XML, and 141 of 188 Java fences import `org.jpos`. `W390` warms a Maven or Gradle seed, but nothing says how an exercise's command reaches it. ⚠️ **Question: where does a jPOS exercise's build (or classpath) and its JUnit report come from?** A build role in the bundle, a launcher the runner image provides, or a corpus-level build file the runner reads rather than the command? That is the framework's call (`AX-04`'s surface and `W390/4`) |
| `ISO-M10/3` | `[structural]` | ISO-21, ISO-23 (iso units 2–4) | ⛔ **The authoring loop numbers a unit's exercises from 1** (`loop.author_page`: `Places(…, len(shipped) + 1)`). `iso-fundamentals` units 2, 3 and 4 already carry a bundled `practice-1` in the archive (W426), so an authored exercise there collides with it. Renumbering the bundled one moves every reader's progress, which is exactly what `loop.py`'s own docstring forbids. ⚠️ **Question: an ordinal offset from the unit's existing practices, read or declared?** The pilot does not touch these units, so this blocks `ISO-23` and not `ISO-22` |
| `ISO-M10/4` | `[structural]` | ISO-19 | ⚠️ **`AX-08/4` restated, measured here.** The manifest lines and the ignore rule are still typed by hand. The ignore rule **cannot even be written before authoring**, because the report path is a per-draft field (`CodeDraft.report`) with no per-runtime convention. ⭐ R19: a fixed report path per runtime would let onboarding generate both the line and the rule |
| `ISO-M10/5` | `[structural]` | ISO-22 | ⚠️ **The `Author` is an in-process Python protocol, and this office is a model session.** There is no console entry point (`AX-08` *For dependents*), and `draft(brief)` is called synchronously inside `author_corpus`. A session can answer it only through a driver that pauses the pass, or through a file-backed author across re-runs. The corpus would then write that driver itself, and every next corpus would retype it (R19). ⭐ `AX-10` was asked to show a driving script. **If the guide lands without one, this blocks the pilot** |
| `ISO-M10/6` | ⛔ **withdrawn by `W453`** (the bands plan nothing now; §1.2) | — | ⚠️ **The bands measure prose only, and this corpus is code-dense.** 34 of 38 units are `short` (at most 2 exercises), one is `standard`, and three plan zero while carrying 7, 5 and 5 Java fences. `jpos-client` 5 and `jpos-server` 5 hold 201 and 202 Java lines. The corpus ceiling is at most 34×2 + 4 = 72 exercises against 188 Java fences, so **at least 116 fences ship as a written reason, not as an exercise.** That is honest under the ledger and is exactly what the user approves at the pilot. It is recorded so the review is framed with it, not discovered after |
| `ISO-M10/7` | question, for the PO | ISO-22's page choice | ⚠️ **The density rule leaves case (a) out of the only review.** `iso-fundamentals` unit 4, the one case-(a) page with real code (9 fences), loses to unit 15 (10) by one fence. Also: this corpus's two test files are the framework's own M7 samples (`W426`), plain-Java and not JUnit, and case (a)'s blanking derivation does not exist yet (`AX-08/2`). Keep the rule, or substitute unit 4 for unit 15? This office keeps the rule until ruled otherwise |
| `ISO-M10/8` | question | ISO-20 | ⚠️ **The execution skill copies the corpus's own build file and authors none, and this corpus has none.** `src/Practice.md` says *"no build tool, no dependency"*. The jPOS version has a source in the material (`2.1.7`, three `pom.xml` fences), but the JUnit version has none, even though 34 fences import `org.junit`. So this office would hand-author the build file and choose a JUnit version nothing in the corpus states. Is that acceptable as a declared hand-authored artifact, or does the framework want the version from somewhere else? |
| `ISO-M10/9` | `[local]` | nothing today | ⭐ A unit whose material is two files (`src/2.md` plus `src/p2.md`) cannot be one `Page`: `Page` takes one path, and `corpus._in_order` refuses two pages on one unit. The practice files carry 0 fences, so no ledger entry is orphaned here, but a passage in `src/p2.md` cannot be a page's own material. Recorded for the next corpus |
| `ISO-M10/10` | `[local]` | nothing | ⚠️ **This document makes `studyforge validate .` RED on this branch** (the bound: one `[unclassified]` finding, `docs/studyforge/m10-plan.md`). `docs/**` was declared on `release/studyforge-integration` and is not declared on the `06df27f` line. `ISO-19` adds it. The same document on `06df27f`'s line cannot be committed GREEN without a manifest edit, and a manifest edit is `ISO-19`'s, not this planning row's |
| `ISO-M10/11` | `[structural]` | ISO-23 | ⛔ **A pass over some of a corpus's pages rewrites the whole ledger with only those pages' entries, and `validate` stays GREEN.** `exercises/ledger.json` is one file per corpus. The `jpos-client` pass, run alone, wrote 67 entries in place of 85 and would have dropped every `iso-fundamentals` entry. `studyforge validate .` still read 0 findings, so an unaccounted lesson page is not a finding. ISO-23 re-ran each pass over every container done so far, and all 38 pages came back `kept`, byte-identical. ⚠️ **Question: should `author_corpus` refuse a ledger that drops pages it was not given, and should `validate` refuse a lesson page the ledger does not account for?** |
| `ISO-M10/12` | ⭐ **ruled by the register**: `jpos-client` unit 1 plans zero (`dda566b`); the framework question stands | ISO-23 (`jpos-client` 1) | ⚠️ **A page that teaches only a tool's convention cannot yield a quiz that clears `Q2`**, because general knowledge answers it without the page. `src/c1.md` was declared a quiz page, was refused at `Q2` on all three attempts, and ships nothing. The shortfall is honest. The question is whether `W453`'s plan should weigh "is this idea the page's own, or the tool's convention?" before it chooses a quiz, so that such a page plans zero with a reason instead of spending three attempts |
| `ISO-M10/13` | `[structural]` | ISO-23's browser reading; any second checkout | ⚠️ **The study server finds its runner by a container name fixed per `source`** (`studyforge-runner-iso-8583-jpos-tutorial`), and that container is the user's, over the main checkout. A worktree or any second checkout of the same corpus therefore falls to host mode, and a code Submit needs the host's own `mvn` (this host has none: exit 127). ISO-23 graded code through a scratch shim that runs the pinned runner image offline. ⚠️ **Question: should the runner's name carry the root (or be overridable), so a second checkout can grade without the user's container?** |
| `ISO-M10/14` | `[local]`, fixed in this diff | ISO-23 | The corpus's `tests/ingest` fixtures addressed units "the authoring pass has not" touched (`jpos-client` 9, `iso-fundamentals` 2 practice-2). Once all 38 units were authored, 5 tests failed (exit 1). The fixtures now take the next free ordinal or address `jpos-client` unit 11 (empty by plan). Every count stays exact. The suite is GREEN, exit 0 |

## 5. Gates taken for this plan

At `06df27f`, in this worktree, host `python3`; framework trees exported with `git archive`, never
checked out into `SF/`.

| gate | framework ref | reading |
|---|---|---|
| `studyforge validate .` | `638e233` (the corpus's pin) | RED, exit 1: `container_api 3` refused in all three container maps |
| `studyforge validate .` | `5e0657ba` | GREEN, exit 0 |
| `python3 -m pytest tests` | `638e233` | RED, exit 1 |
| `python3 -m pytest tests` | `5e0657ba` | RED, exit 1: one test, the stub/pin drift (`ISO-M10/1`) |
