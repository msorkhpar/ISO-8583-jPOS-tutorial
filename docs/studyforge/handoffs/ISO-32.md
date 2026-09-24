# ISO-32 handoff: the corpus re-onboarded on the installed library, and made release-ready

**Office** `po-int`. **Branch** `int/m10-iso-23`, from `e456e77`. **Milestone** M11, step 11.5
(the corpus rows). **Framework** `studyforge` 0.1.0, built from the release tip `6ca2c94c`
(W460 merged, so it is the tip after W460 and not `9fd6c2d2`).

| commit | what |
|---|---|
| `54fb207` | re-onboard on the installed library at `6ca2c94c`, and rebuild the site with it |
| `62d7400` | `ONBOARDING.md` leaves the root, to `docs/archive/ONBOARDING.md` |
| `ec8fb96` | the README opens with a stranger's reading list; `ingest/read.py` stops citing README line numbers |
| this commit | this handoff |

## How the library was installed

From a scratch directory, with no framework checkout on the import path:

1. `git archive 6ca2c94c` of the studyforge main checkout, extracted to scratch.
2. A wheel built from it with the host's own setuptools (`pip wheel --no-deps
   --no-build-isolation --no-index`), nothing fetched. The export was then deleted.
3. A fresh venv (`--system-site-packages`, only so the host's pytest is importable; the host
   Python has no `studyforge`), and the wheel installed with `pip install --no-index --no-deps`.
4. `PYTHONPATH` unset, the venv first on `PATH`. `import studyforge` resolves inside the venv.

The corpus references none of this. Its pin names the version and the commit, and its stubs name
`python3 -m studyforge.skills.documents <skill>`.

## Step 1: re-onboard

`hand_edited('.')` read `[]` before. Then:
`reonboard('.', framework_commit='6ca2c94cebd8fb903ab931ba32f4e297e68483f5').write('.', regenerate=True)`.
Then `hand_edited('.')` read `[]` again.

- The pin: `"where": "installed"`, version `0.1.0`, commit `6ca2c94c…`. The three stubs,
  `installed.json` and `ONBOARDING.md` moved with it. `corpus.json` did not change: it stays at
  `corpus_api` 4 with no `narration` key, which means narration is on. That question is still
  open with the user for this corpus, so nothing was set.
- **Each stub resolves through the installed package.** For `reconnaissance`, `adapter` and
  `onboarding`, the command the stub names prints bytes identical to the wheel's
  `studyforge/skills/<name>/SKILL.md`.
- **The rebuild** (`build . --out .`) replaced only `.studyforge/assets/page.css` and
  `page.js`. Every other page it wrote was byte-identical to the committed one. Before that, a
  build into scratch was compared with the tree file by file, which is how this was checked.

### `git grep -n '\.\./studyforge'`

It prints three lines, all in `docs/studyforge/m10-plan.md` (lines 36, 62 and 190). **They
stay.** That file is the M10 plan, a dated record of the commands that milestone ran in the
development workspace. The corpus runs none of them, and no generated file, stub, test or
ingest module names a sibling. Rewriting them would falsify the record.

⚠️ Outside the repository, `ISO-8583-jPOS-tutorial-wt/studyforge` is an untracked symlink to
`../studyforge`. It exists so the old sibling paths resolved from the worktree. The corpus no
longer needs it. This office did not remove it: it is outside scratch, and it is the user's to
prune.

## Step 2: ONBOARDING.md is archived, but NOT under `archive/`

⛔ **The brief said to move it into the corpus's `archive/`, and that turns `validate` RED.**
In this corpus, `archive/` is the studyforge archive: the three container maps and their
documents. The move was tried with `git mv` and never committed. On it, `validate` exited 1
with one `[archive-stray]` finding: *"archive/ONBOARDING.md … sits beneath archive/ and is not
an archive member … It is refused, not skipped."*

⭐ **So it went to `docs/archive/ONBOARDING.md`** (`git mv`, so the history follows it).
`docs/**` is already declared `not_material`. `validate` is GREEN on it.

**Links.** No markdown link in the repository pointed at `ONBOARDING.md`, so nothing dangles.
Four places still name it, and none of them is a link:

- `corpus.json`'s `not_material` glob `ONBOARDING.md`, and `.studyforge/installed.json`'s record
  of it. Both are generated, so hand-editing them would be a finding (R19). The glob now
  matches nothing, which is harmless.
- `tests/test_framework_pin.py`'s failure message, *"install the pinned version (ONBOARDING.md
  says how)"*. It is generated. The README now says how. See `ISO-32/1`.
- `docs/studyforge/handoffs/ISO-25.md`, a dated record.

⚠️ **The next re-onboarding writes `ONBOARDING.md` back at the root** (`ISO-32/1`).

## Step 3: the README

The README was the tutorial's own curriculum record. The adapter reads it too: `ingest/read.py`
takes every series and entry above the label linking `TestCases.md`. It cited no process id, so
nothing was removed. It now opens, under the title, with:

- what the repository is;
- what you need, including how to install the library as a wheel and check it against the pin;
- how to open the committed site with nothing running;
- how to serve it, with `studyforge serve . --site .`;
- how to rebuild it;
- a table of what needs Docker;
- how to print the skill procedures from the installed package.

A `## Contents` heading then introduces the unchanged curriculum.

- ⛔ **The curriculum is unchanged, and this was measured.** The introduction carries no level-1
  label, no entry-shaped line and no `](src/` link. `python3 -m ingest . 2026-09-21` (the
  archive's recorded `ingested` date) rewrote the archive byte for byte: `git status` was
  clean apart from the README.
- **`ingest/read.py`'s docstring** said *"Lines 1-310"* of the README. The introduction moved
  that, so the docstring now names the label instead. That is the one hand-written file, so
  editing it is not a finding.
- **Serve was checked:** `serve . --site . --port 8791` from the installed library answered
  `GET /index.html` with 200 on loopback, then was stopped. The user's site on 8770 was not
  touched.

## Gates, at `ec8fb96`, installed library `6ca2c94c`, host

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m pytest tests/test_framework_pin.py` (the generated pin test) | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0 (no stale-narration finding, so nothing to re-synthesise) |
| `python3 -m studyforge.cli plan .` | GREEN, exit 0 |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0, and the tree clean afterwards |
| `hand_edited('.')` | `[]` |
| the corpus suite, `python3 -m pytest tests` | GREEN, exit 0 |

## Findings against the framework

| id | where | what |
|---|---|---|
| `ISO-32/1` | `skills/onboarding/artifacts.py` `READER_DOC`, `onboard` | ⚠️ **The reader document's path is fixed at the root.** No manifest field can place it elsewhere, so a corpus that archives it gets it back at the root on its next `reonboard`. The generated pin test also still points readers at it (*"ONBOARDING.md says how"*). The user's ruling to archive `ONBOARDING.md` cannot hold across a regenerate until onboarding can place the document, or stop writing it |
| `ISO-32/2` | `skills/onboarding` `hand_edited` | ⚠️ **A generated file that was moved away is not reported.** With `ONBOARDING.md` gone from where `installed.json` records it, `hand_edited('.')` still read `[]`. Removing a generated file is a change to a generated artifact, and this proof does not see it |
| `ISO-32/3` | `skills/execution` (the generated `EXECUTION.md` and `.studyforge/execution/compose.yaml`) | ⚠️ **Generated stranger-facing text cites process ids:** `SK-09/2` in `EXECUTION.md`, and *"§8.1 ruling 4"* in both files. The README now links `EXECUTION.md`, so a stranger reads them. They are generated, so they were not edited (R19) |
| `ISO-32/4` | the generated pin test and `ONBOARDING.md`'s install prose | ⚠️ **The generated tests need pytest, and nothing says so.** The wheel declares no test dependency, so a venv holding only the wheel cannot run `tests/test_framework_pin.py`. This office used `--system-site-packages` to reach the host's pytest |
| `ISO-32/5` | the brief, not the framework | ⚠️ **`archive/` is the studyforge archive's directory in every corpus**, so "move it to the corpus's archive" cannot mean that directory. `validate` refuses a stray there. A retired document needs another home, as here under `docs/archive/`, or the archive-branch form the framework's own ruling used |

`REL-05/1` still stands: the wheel carries no commit, so the pin's commit was checked for shape
only.

## Step 4: advancing main and pruning. PREPARED, NOT RUN

⛔ **Nothing below was run.** `main` was not moved, and no branch or worktree was touched. The
user approves these.

**The final tip** is the commit that adds this handoff, the child of `ec8fb96` on
`int/m10-iso-23`. Before running anything, check that `git rev-parse int/m10-iso-23^` prints
`ec8fb96…` and that `int/m10-iso-23` is the tip reported with this handoff.

**Advance main** (`main` is not checked out anywhere; the corpus main checkout is detached at
`e456e77`, so this does not move the served site). From the corpus main checkout:

```
git merge-base --is-ancestor main int/m10-iso-23 && git branch -f main int/m10-iso-23
```

**Every local branch**, with `git merge-base --is-ancestor <tip> ec8fb96`. The exit code is the
same against the final tip, because that tip is `ec8fb96`'s child:

| branch | tip | exit |
|---|---|---|
| `int/m10-iso-23` | `ec8fb96` (the final tip once this handoff lands) | 0 |
| `int/m10-pilot-quiz` | `424e55a` | 0 |
| `int/m10-step-10.4-plan` | `acde021` | 0 |
| `int/round10-iso09` | `e3c6872` | 1 |
| `int/round11-repin` | `4643c6a` | 1 |
| `int/round12-iso12-gate` | `300b9f2` | 1 |
| `int/round13-iso14` | `dadb703` | 1 |
| `int/round14-repin-iso12-iso14` | `a5cd5f7` | 1 |
| `int/round14-repin-iso14` | `e9ffbaa` | 1 |
| `int/round15-iso12-commit` | `5d788b1` | 1 |
| `int/round16-repin-iso06-iso13` | `322d40b` | 1 |
| `int/round17-repin-iso15` | `7b9577f` | 1 |
| `int/round18-repin-iso17` | `663c9de` | 1 |
| `int/round6-m6-opens` | `6f21906` | 1 |
| `int/round7-iso04` | `2a31123` | 1 |
| `int/round8-iso05` | `1725884` | 1 |
| `int/round9-iso06-08` | `ebeaef1` | 1 |
| `main` | `66ba23b` | 0 |
| `rebuild/clean-run` | `77535e6` | 0 |
| `release/studyforge-integration` | `c3eb94f` | 1 |

**Delete the merged ones** (each re-checked immediately before its deletion). Run these after
`main` has advanced:

```
git merge-base --is-ancestor int/m10-pilot-quiz main && git branch -d int/m10-pilot-quiz
git merge-base --is-ancestor int/m10-step-10.4-plan main && git branch -d int/m10-step-10.4-plan
git merge-base --is-ancestor rebuild/clean-run main && git branch -d rebuild/clean-run
```

- `int/m10-iso-23` is merged too, but the linked worktree `ISO-8583-jPOS-tutorial-wt/int` has
  it checked out. It can be deleted only after that worktree is removed, and removing it is
  the user's call. Afterwards:
  `git worktree remove ISO-8583-jPOS-tutorial-wt/int && git branch -d int/m10-iso-23`.

⛔ **Unmerged, never to be deleted by this procedure: 15 branches.** `release/studyforge-integration`
is the old integration line, from before the clean rebuild. Its merge-base with the final tip
is `66ba23b`, the old `main`, and its history is reachable from nothing else. Thirteen of the
fourteen `int/round*` branches are ancestors of it. The exception is `int/round14-repin-iso12-iso14`,
which is an ancestor of neither line. Whether that old line is kept, or given an archive name,
is the user's decision.
