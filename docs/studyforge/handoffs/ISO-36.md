# ISO-36 handoff: one regeneration onto the release framework

**Office** `po-int`. **Branch** `int/iso-36-regenerate`, from `main` `54d4208`. **Milestone**: the release.
**Framework** `studyforge` 0.1.0, built from `037a3eec` (W479 merged). This row first ran on
`e29cf2c9`. When the register's message moved it to `037a3eec`, that pass was discarded
uncommitted, and the whole row was run again. **Toolchain** `code-server-toolchain` at `497b4cd`.
**narrate-service** at `62725dd`, whose `consuming.json` is byte-identical to `00f2405`'s.

| commit | what |
|---|---|
| `3d483f5` | re-onboard on `037a3eec`, the execution regenerate through the skill, the adapter's trust change and ingest, and the build |
| this commit | this handoff |

## How the library was installed

`studyforge` was cloned into scratch with `git clone --no-local` and checked out at `037a3eec`.
A wheel was built from that clone with `pip wheel --no-build-isolation --no-index`. It went into a
fresh venv with `--no-index --no-deps`. The venv has `--system-site-packages` only for the host's
pytest. Every command ran with `PYTHONPATH` unset. `import studyforge` resolved inside the venv, and
`library.commit()` reported `037a3eec9dc3fc7a245cbc285a4a43d7947f3ea4`.

## The editor image

From `code-server-toolchain` `497b4cd`, following its README's *The editor image*:
`TC_KEEP_IMAGES=1 flock /tmp/studyforge-container-gate.lock python3 docker/editor/build.py --runtimes java,maven`.
It exited 0, and its activation and confinement proofs held. It tagged
`code-server-toolchain/editor:java-maven-amd64-f67414139266`, and `--print-tag` printed the same tag.
The runner tag already existed, so no runner was rebuilt, and no other image was built.
⚠️ `build.py` has no way to pass `--pull never`. It runs `docker build` without `--pull`.

## What was done

1. `reonboard('.', framework_commit='037a3eec…').write('.', regenerate=True)`. `hand_edited` read
   `[]` before and after.
2. The execution regenerate, following the skill's SKILL.md step 1 and 5a:
   - `generate` read the toolchain's `consuming.json` from a `git archive` of `497b4cd` and
     narrate-service's from `62725dd`;
   - then `write`, `record_runner` and `record_editor` ran, with the `ask` from `record`'s
     docstring.
   - ⛔ `write` first refused the committed `EXECUTION.md` as "a reader's document that this skill
     did not write" (`ISO-36/2`). As the refusal says, the file was moved aside into scratch, and
     the skill was run again.
3. In `ingest/practices.py`, `WORKSPACES` for `src/p1.md` and `src/p2.md` changed from
   `authoritative` to `advisory`. Then `python3 -m ingest . 2026-09-21` ran, keeping the archive's
   recorded date. The adapter's own audit reported valid.
4. `build . --out .`.

**W340 `curriculum`: left undeclared.**
- The reconnaissance skill writes the block into a survey's draft.
- The adapter skill uses it only where `corpus.json` already declares it, which is a scaffolded
  adapter's case.
- `reonboard` drafts none.
- Nothing in the skills says an onboarded corpus with a hand-written adapter should adopt it, and
  W340/4 calls adopting it optional.

## Actual diff against the expected one

| expected | actual |
|---|---|
| the pin and stubs move | ✅ `pin.json`, the three stubs and `installed.json` moved. The generated `tests/test_framework_pin.py` and `tests/test_non_destructive.py` were rewritten by the new library |
| `editor.env` carries the new tag | ⚠️ It carries `…/editor:java-maven-amd64-0909bea405f2`, not `…-f67414139266`. See `ISO-36/1` |
| the two practices' `trust` becomes `advisory`, with the page sentence to match | ✅ These moved: the two archive documents (one line each), `ingest/practices.py`, and the unit 2 and unit 4 pages. Their sentence is now "Checked by tests that ship with this material. They have not been proven to catch a wrong answer." |
| the clip signal file appears | ✅ `.studyforge/assets/narration-clips.js` = `"present"`, since this corpus commits its 1921 clips. 38 pages link it |
| generated text changes where W469–W478 changed the framework's prose | ✅ Changed: `corpus.json` (one `why`), `docs/archive/ONBOARDING.md`, `ingest/__init__.py`, `__main__.py`, `emit.py`, `tests/ingest/test_emit.py`, `page.css`, `page.js`, `EXECUTION.md` and the practice pages' "no-editor" sentence (43 places). `ONBOARDING.md` gains W479's `## Narration`, worded for committed clips |
| `runner.env` unchanged | ⚠️ Its tag is unchanged. Its header comment line changed with the new marker text (`ISO-36/3`) |
| beyond it | ⚠️ New files from the execution skill: `.studyforge/execution/instance.env` (the four instance defaults) and `.studyforge/execution/written.json`. `compose.yaml` interpolates the project, the port and both container names. `EXECUTION.md` prints the editor build and tag lines with `--prime`, and adds `--env-file …/instance.env` to "Bring it up" |

## Gates

On the host, installed library `037a3eec`, in this worktree.

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m pytest tests/test_framework_pin.py`, and `python3 tests/test_framework_pin.py` | GREEN, exit 0, both |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0: 0 findings, no `derivation-record` |
| `python3 -m studyforge.cli plan .` | GREEN, exit 0 |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0. After the commit, a second build left the tree clean |
| the corpus suite, `python3 -m pytest tests`, before the commit | ⛔ RED, exit 1. Only `test_nothing_that_already_existed_changed_but_what_is_declared` failed, and it named `ingest/practices.py` (`ISO-36/5`) |
| the corpus suite at `3d483f5`, after the commit | GREEN, exit 0, 39 passed |
| `hand_edited('.')` | `[]` |
| `docker compose --env-file runner.env --env-file editor.env --env-file instance.env -f compose.yaml config --images`, with neither image variable in the environment | exit 0. It names `…/runner:java-maven-amd64-29d0605bb762` (unchanged) and `…/editor:java-maven-amd64-0909bea405f2`. ⛔ **RED against the brief**, which wanted `…-f67414139266` (`ISO-36/1`) |

## Findings

| id | where | what |
|---|---|---|
| `ISO-36/1` | the execution skill against the toolchain's `provides` 3 | ⛔ **High: blocks the redeploy.** The toolchain's `consuming.json` at `497b4cd` declares `editor.prime` (`folded_into_tag: true`). So the skill asks for the editor's tag with `--prime`, and it records the primed tag **`java-maven-amd64-0909bea405f2`**. The brief, the toolchain README's build line and W468's handoff all name the unprimed tag `…-f67414139266`, which is the image built here. **No image `…-0909bea405f2` exists on this host.** Before the site is moved onto this tip, the primed editor needs building: `python3 docker/editor/build.py --runtimes java,maven --prime <corpus>/.studyforge/execution/prime`, as the new `EXECUTION.md` prints it. Otherwise the tag in `editor.env` names nothing. This row did not build it: the brief allowed no other image. Register's ruling |
| `ISO-36/2` | studyforge `skills/execution/onboard.py` (`_is_somebody_elses`, `write`) | Medium, framework. The skill decides whether it wrote `EXECUTION.md` by looking for the exact `GENERATED` sentence. The prose pass changed that sentence (the "(R19)" wording), so the skill refuses its own earlier output. That affects every corpus onboarded before the change. `generated_here` or `written.json` could answer the question instead. ⚠️ Also, `write` is not atomic: it had already rewritten `compose.yaml` before it refused |
| `ISO-36/3` | `runner.env` | Low. The tag is unchanged. Only the generated header line moved with the new marker text |
| `ISO-36/4` | the brief's expected diff | Low. `instance.env`, `written.json`, the `compose.yaml` interpolations and the primed `EXECUTION.md` lines are all the execution skill's current output. `page.css` and `page.js` also changed with the framework |
| `ISO-36/5` | the generated `tests/test_non_destructive.py` | Low. Before the commit, the corpus's own adapter edit (`ingest/practices.py`) counts as undeclared. The test reads the working tree against `HEAD`, so any adapter-data change is RED until it is committed. It read GREEN after the commit |
| `ISO-36/6` | the toolchain's `docker/editor/build.py` | Low. It takes no `--pull never`. It runs `docker build` without `--pull`, and its `face` stage uses `ADD --checksum` |
| `ISO-36/7` | the host's images | Low. The rebuild put `…-f67414139266` on a new image ID (`eff53864…`). The earlier image `ee8dc26f…` is now untagged and was left in place |
| `ISO-36/8` | `ingest/__init__.py` (generated) | Low. It still says it depends on "`studyforge` as a sibling checkout", but the corpus is pinned to an installed library |
| `ISO-36/9` | studyforge `archive/process:workspace.json` | Low. It still pins the toolchain at `fb5cb13` and narrate-service at `00f2405`. This row used `497b4cd` and `62725dd`, as the brief says |

## For the register

- `main` was not moved, and no branch or worktree was touched. The corpus main checkout and its two
  containers were not touched. No container was started by this row beyond the build's own proofs,
  and none is left running. Nothing was pushed.
- The site still runs `…/editor:java-maven-amd64-0c712ff89060`. Advancing to this tip needs the
  primed editor image of `ISO-36/1`, then the editor recreated on it. The runner needs nothing.
