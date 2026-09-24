# ISO-34 handoff: the editor's tag recorded, and the fixed non-destructive check

**Office** `po-int`. **Branch** `int/m10-iso-23`, from `aae318e`. **Milestone** M11, step 11.5.
**Framework** `studyforge` 0.1.0, built from the release tip `2fe13269` (W464 merged).
**Toolchain** `code-server-toolchain` at `fb5cb13`, and narrate-service at `00f2405`. Both are the
commits that tip's `workspace.json` pins.

| commit | what |
|---|---|
| `f97b166` | re-onboard on `2fe13269`, then the execution regenerate, recording both tags |
| this commit | this handoff |

## How the library was installed

This is the same method as ISO-33. A `git archive` of `2fe13269` was extracted to scratch. A wheel
was built from it with the host's setuptools, with nothing fetched. The export was deleted, and the
wheel was installed into a fresh venv with `--no-index`. The venv uses `--system-site-packages`
only for the host's pytest (ISO-32/4 still stands). `PYTHONPATH` was unset and the venv came first
on `PATH`. `import studyforge` resolved inside the venv.

## Step 1: re-onboard first (W464/3)

Before: `hand_edited('.')` read `[]`.

```
reonboard('.', framework_commit='2fe132690660f2f20d44330a4883dddbd7f32a59').write('.', regenerate=True)
```

`onboarding_doc` was not passed, because it stays as settled in `corpus.json`, and `corpus.json`
did not change. What moved:

- `tests/test_non_destructive.py` imports `studyforge.skills.execution.generated_here`. Its
  `_undeclared` now excludes the execution skill's own output, and its message says so.
- `.studyforge/pin.json`, the three skill stubs, `docs/archive/ONBOARDING.md` (the commit
  sentence) and `.studyforge/installed.json` (their hashes) moved to the new commit.
- `tests/test_framework_pin.py` rewrote byte-identical: it names no commit.

## Step 2: the execution regenerate, through the skill

The work followed `SKILL.md` step 1 as printed, which the corpus could not do in ISO-33.
`generate(manifest, editor_text=…, root=., narration_text=…)` read each sibling's
`consuming.json` from a `git archive` at its pin. Then `write(execution, corpus)` ran. Then
`record_runner` and `record_editor` ran, with the `ask` from `record`'s docstring, against the
toolchain export.

- `EXECUTION.md` changed in "Record both tags" (renamed from "Record the runner's tag") and
  "Bring it up", which now passes two `--env-file`s.
- New: `.studyforge/execution/editor.env` =
  `EDITOR_IMAGE=code-server-toolchain/editor:java-maven-amd64-0c712ff89060`.
- `runner.env` (`…/runner:java-maven-amd64-29d0605bb762`), `compose.yaml`, `toolchain.json` and
  the prime rewrote byte-identical.
- No image was built or rebuilt, and no container was started.

⭐ **The actual diff matches W464's expected diff, with nothing beyond it.**

## Gates

On the host, installed library `2fe13269`, in this worktree.

| gate | reading |
|---|---|
| the corpus suite, `python3 -m pytest tests`, on the uncommitted regenerate | GREEN, exit 0. ⭐ ISO-33/1 is fixed |
| `tests/test_non_destructive.py`, with the regenerate staged | GREEN, exit 0 |
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m pytest tests/test_framework_pin.py` | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0 |
| `python3 -m studyforge.cli plan .` | GREEN, exit 0 |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0. The tree was clean afterwards: the build changed nothing |
| `hand_edited('.')` | `[]` |
| the corpus suite at `f97b166`, after the commit | GREEN, exit 0 |
| the printed compose command, with its verb swapped for `config --images` (`--env-file` runner, `--env-file` editor, `-f` compose), with neither image variable in the environment | GREEN, exit 0. It named exactly the recorded editor and runner tags |

⭐ **These are the two tags the site runs.** A read-only `docker ps` shows the user's
`…-editor-1` on `…/editor:java-maven-amd64-0c712ff89060` and the runner on
`…/runner:java-maven-amd64-29d0605bb762`. The switch-over ISO-33 prepared has already happened, so
nothing in this row needs a restart.

## Findings

| id | where | what |
|---|---|---|
| `ISO-34/1` | the brief's expected diff | Low. "The pin and stubs move" held, but the generated pin test (`tests/test_framework_pin.py`) did not move, because it carries no commit. That is correct and is noted only so nobody reads its absence from the diff as a miss |
| — | W464/2 | Still stands and is visible in this corpus. `EXECUTION.md` line 59 reads "Pass `--prime …` to the builds above", plural, but only the runner's build line carries the flag |
| — | ISO-33/4, ISO-33/5, W464/1, W462/1, ISO-32/4, REL-05/1 | Still stand |

## For the register

- `main` was not moved, and no branch or worktree was touched. The corpus main checkout and its
  containers were not touched. ISO-32's prepared advance procedure still applies, with this
  handoff's commit as the tip.
- The site's containers already match the recorded tags, so advancing the main checkout to this
  tip needs no image and no restart.
