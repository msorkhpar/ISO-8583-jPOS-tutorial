# ISO-33 handoff: the corpus regenerated against the release-ready framework and toolchain

**Office** `po-int`. **Branch** `int/m10-iso-23`, from `f329855`. **Milestone** M11, step 11.5.
**Framework** `studyforge` 0.1.0, built from the release tip `ebbc37d4` (round 185). **Toolchain**
`code-server-toolchain` at `fb5cb13`, the commit that tip's `workspace.json` pins (REL-13 and W463
merged).

| commit | what |
|---|---|
| `8d11fa6` | re-onboard with `onboarding_doc` settled, regenerate the execution files, re-record the runner, rebuild the site |
| this commit | this handoff |

## How the library was installed

As in ISO-32: a `git archive` of `ebbc37d4` extracted to scratch, a wheel built from it with the
host's setuptools and nothing fetched, the export deleted, and the wheel installed into a fresh
venv (`--system-site-packages`, only for the host's pytest; ISO-32/4 still stands). `PYTHONPATH`
unset, the venv first on `PATH`, `import studyforge` resolving inside the venv.

## Step 1: re-onboard, with the reader document archived

Before: `hand_edited('.')` printed one sentence, the root `ONBOARDING.md` gap (W461/2, as
expected). Then:

```
reonboard('.', settle={"onboarding_doc": "docs/archive/ONBOARDING.md"},
          framework_commit='ebbc37d4f3cc8177c18435b457089373718ebded').write('.', regenerate=True)
```

- `corpus.json` rose to `corpus_api` 6 and carries `onboarding_doc`. Its reader glob is now
  `docs/archive/ONBOARDING.md`; the stale root glob is gone.
- Nothing is written at the root: there is no `ONBOARDING.md` there.
- `hand_edited('.')` reads `[]`.
- The generated pin test says `(docs/archive/ONBOARDING.md says how)`.
- The pin, the three stubs, `installed.json` and the archived reader document moved to the new
  commit. The adapter scaffold's `ingest/emit.py` and `tests/ingest/test_emit.py` lost their
  process ids (W462), through this regenerate.

## Step 2: the execution files, through the skill

`generate(manifest, editor_text=<toolchain consuming.json at fb5cb13>, root=., narration_text=<narrate-service
consuming.json at the pinned commit>)`, then `write`. The contracts were read from a `git archive`
of each sibling at its pinned commit, in scratch.

- `EXECUTION.md` and `.studyforge/execution/compose.yaml` changed in their prose only: the
  §8.1 "ruling N" and `SK-09/2` citations are gone (W462). No key or value line of the compose
  file changed.
- The prime and `toolchain.json` rewrote byte-identical.

## Step 3: the tags

- **Runner, recorded by step 5a** (`record_runner`, which ran the contract's `tag_from` with the
  prime flag in the toolchain export):
  `code-server-toolchain/runner:java-maven-amd64-29d0605bb762`. `runner.env` changed in its one
  tag line. (REL-13's `…-a5c56ce13905` is the unprimed tag; this corpus's is primed, so it differs.)
- **Editor**, from its `tag_from` in the same export:
  `code-server-toolchain/editor:java-maven-amd64-0c712ff89060`. ⚠️ Nothing in the corpus records
  it (`ISO-33/3`).
- **Both built** from the export, under `/tmp/studyforge-container-gate.lock` with
  `TC_KEEP_IMAGES=1`: the runner with `--prime` pointing at this corpus's prime, then the editor.
  Both GREEN, exit 0. The editor build's confinement gate reported no missing and no extra
  keybinding.
- ⛔ **Untouched:** `…/editor:java-maven-amd64-d961830755e8`, `…/runner:java-maven-amd64-7a5a2f2aacba`,
  and both of their running containers. The site on 127.0.0.1:8770 answered 200 afterwards.
  The editor tag `…-0c712ff89060` already existed (W463 built it); this rebuild re-tagged it and
  nothing was removed.

## Step 4: the site, rebuilt in place

`build . --out .` replaced `.studyforge/assets/page.css` and `page.js` (comments only: W462's
process ids gone). Every other page it wrote was unchanged.

## Gates

On the host, installed library `ebbc37d4`, at `8d11fa6` in this worktree unless said otherwise.

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m pytest tests/test_framework_pin.py` (the generated pin test) | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0; no stale-narration finding, so nothing was re-synthesised |
| `python3 -m studyforge.cli plan .` | GREEN, exit 0 |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0, and the tree clean afterwards |
| `hand_edited('.')` | `[]` |
| the corpus suite, `python3 -m pytest tests` | GREEN, exit 0. ⚠️ Read before the commit, on the uncommitted regenerate, it was RED, exit 1, on `test_non_destructive.py::test_nothing_that_already_existed_changed_but_what_is_declared`, naming `EXECUTION.md` (`ISO-33/1`) |
| runner build at `…-29d0605bb762`, primed | GREEN, exit 0 |
| editor build at `…-0c712ff89060` | GREEN, exit 0 |
| editor start on the new tag, 127.0.0.1:8544 | GREEN: `up --wait` exit 0, container healthy, `GET /healthz` 200; the run index named it as the corpus's editor |
| graded practice through the new runner (`iso-fundamentals/unit-05/practice-prose`) | GREEN: Run exit 0; Submit on the starter exit 1 with every case failed; Submit on the bundle's reference exit 0 with every case passed, recorded as passed |

**How the practice was run, beside the live site and without touching it.**

- A `git archive` of `8d11fa6` in scratch, built there so `site.json` existed.
- The generated compose file brought up under its own project name (`iso33-probe`), with
  `--pull never` and `EDITOR_IMAGE` set to the new editor tag. A scratch override file changed
  only the editor's host port (8544) and the runner's container name, because both are literals
  that the user's project already holds (`ISO-33/4`).
- `studyforge serve . --site . --port 8793` from the installed library. It ran in-process, with
  `routes.runs.container_for` and `editor_container_for` substituted to return the probe's
  names (`ISO-33/5`). Run, Submit and the practice-editor route were POSTed to it.
- The runner's mode read `container`, and the host has no `mvn` or `java`, so the run was
  in the runner, offline (`mvn -o`, `network_mode: none`).
- Afterwards the server was stopped and `docker compose … down -v` removed the probe's two
  containers, its network and its three volumes, all under the lock. The export was deleted.

## Findings

| id | where | what |
|---|---|---|
| `ISO-33/1` | the generated `tests/test_non_destructive.py` (onboarding), with the execution skill | ⚠️ **A regenerate by the execution skill reads as an R3 breach until it is committed.** `EXECUTION.md` is generated at the root by the execution skill, but it is not in onboarding's install record, not in the plan's output and not under `.studyforge/`. So the working-tree check names it as an undeclared rewrite. Committing clears it, which means the check is really only reading it against `HEAD`. The check should take the execution skill's generated paths (its `GENERATED`/`NOT_MATERIAL`) as declared |
| `ISO-33/2` | `skills/execution/SKILL.md`, step 1 | ⚠️ **The procedure names `Execution.for_corpus(manifest, ...)`, which does not exist.** The entry point is `generate(manifest, editor_text=, root=, narration_text=)`. Nor does the procedure say where `editor_text` and `narration_text` come from: the pinned siblings' `consuming.json` files. The procedure also cannot be followed as printed |
| `ISO-33/3` | `skills/execution`, step 5a | ⚠️ **Only the runner's tag has a record step.** The editor's tag is an interpolation (`EDITOR_IMAGE`) that the corpus records nowhere. So "re-record the editor tag" has no file to land in. The tag the site runs is known only to the running environment, and a switch-over has to take it from a person |
| `ISO-33/4` | `skills/execution/composefile.py`, the rendered compose file | ⚠️ **The editor's host port (`127.0.0.1:8443`) and the runner's `container_name` are literals.** The procedure says per-project values arrive as interpolations with defaults. As rendered, a second instance of one corpus cannot come up beside the first without an override file. That is the verification a pin advance needs. This is related to REL-13/3 |
| `ISO-33/5` | `serve.routes.runs`, `execute.commands.container_for` | ⚠️ **A served instance finds its runner and editor by `source` alone.** There is no flag or data to point one at a second runner, so a new runner tag cannot be proved through `serve` beside a live site without substituting the lookup in-process, as this office did in scratch |
| — | W462/1, ISO-32/4, REL-05/1 | Still stand |

## For the register

- **To switch the site over** (the register's step, after its own verification): in the corpus
  main checkout, once it is at this branch's tip, run
  `docker compose --env-file .studyforge/execution/runner.env -f .studyforge/execution/compose.yaml up -d --wait --pull never`
  with `EDITOR_IMAGE=code-server-toolchain/editor:java-maven-amd64-0c712ff89060`. That replaces
  both of the user's containers. Keep the old images until the site is confirmed.
- `main` was not moved, and no branch or worktree was touched. ISO-32's prepared advance and
  pruning procedure still applies, with this handoff's commit as the tip.
- ⚠️ The W463 handoff (`d4b2756f`) is on studyforge's `docs/W463-handoff` and
  `chore/po-round185-archive`, not on `archive/process` as of this row. It was read from the commit.
