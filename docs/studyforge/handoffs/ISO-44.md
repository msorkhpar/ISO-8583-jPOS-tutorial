# ISO-44 handoff: `instance.env` becomes the publisher's file

**Office** `po-int`. **Branch** `int/iso-44-publish`, from `main` `b49766a` (ISO-43).
**Framework** `studyforge` 0.1.0, built from `f72a1dde` (W505 merged). **Toolchain**
`code-server-toolchain` at `6e226d0`, unchanged.

| commit | what |
|---|---|
| `6f76207` | re-onboard, the execution regenerate, the instance refreshed, the site re-staged, and the build |
| this commit | this handoff |

## What was done

1. **Install.** `studyforge` was cloned into scratch at `f72a1dde`, and a wheel built from the
   clone was installed into a fresh venv. `PYTHONPATH` was unset. `library.commit()` reported
   `f72a1dde0fc04ba3131fcc31b50460b326dac858`. Then `reonboard(…, framework_commit=…)` ran.
   `hand_edited` read `[]` before and after.
2. **The execution regenerate** ran through the skill against a `git archive` of `6e226d0`.
   `compose.yaml` gains:
   - a `preflight` service on the site image, with no network and a read-only corpus, that runs
     `preflight /corpus`;
   - a `depends_on` on the preflight's successful completion for the site, the editor and the
     runner, required when `STUDYFORGE_PREFLIGHT` is set;
   - a site healthcheck that fetches the site's own page.
   
   `EXECUTION.md` follows these changes.
3. **`record_instance(execution, root)` ran once, with no port handed.** Only the header comment
   changed, to "from then on this file is yours…". The values are kept: site 8770 and editor
   8443, and the project and container names are unchanged (W505/1).
4. **Step 5b.** `stage_site('.')` gave the **new site tag `studyforge-site:0.1.0-0d0cfd0720c3`**.
   It was `…-e173e33edf3f`. `site.env` gains `STUDYFORGE_PREFLIGHT=true`.
   - The image was built under the gate lock with `docker build --pull=false` and the staged
     argv's file, tag and context.
   - ⚠️ `docker build` has no `never` value. `--pull=false` is its equivalent, and the base
     resolved from the local store by its digest.
5. **Ingest** changed no archive byte. Then `build . --out .` ran.

## The tags

| image | tag |
|---|---|
| editor | `code-server-toolchain/editor:java-maven-amd64-418405a27fd2`, unchanged |
| runner | `code-server-toolchain/runner:java-maven-amd64-57f9563fbd20`, unchanged |
| site | `studyforge-site:0.1.0-0d0cfd0720c3`, new |

## Gates

On the host, installed library `f72a1dde`, in this worktree.

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m pytest tests/test_framework_pin.py` | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0, 0 findings |
| `python3 -m studyforge.cli plan .` | GREEN, exit 0 |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0. After the commit, a second build left the tree clean |
| the corpus suite, before the commit | GREEN, exit 0, 39 passed |
| the corpus suite at `6f76207` | GREEN, exit 0, 39 passed |
| `hand_edited('.')` | `[]` |
| the compose `config --images` (four env files; no image variable, profile or preflight flag in the environment) | GREEN, exit 0. It named the runner `…-57f9563fbd20`, the editor `…-418405a27fd2`, and the site `…-0d0cfd0720c3` twice, because the `preflight` service runs the site image |

## The live reading, on a scratch compose

- **The setup.** A `git archive` of `6f76207` went into scratch. `record_instance` gave it
  project `iso44-live`, editor port 18443 and site port 18793. `stage_site` gave the same tag.
- Everything ran with `--pull never`. Afterwards the compose was brought down with `down -v`, and
  the copy was deleted.
- The user's three ISO containers were not touched. `docker events` shows no activity on their
  runner.

| reading | result |
|---|---|
| **A shared port is refused by name, and nothing starts.** `instance.env` was set with site port = editor port = 18443 | `up -d --wait` exited 1: `service "preflight" didn't complete successfully`. The preflight printed: *".studyforge/execution/instance.env: STUDYFORGE_EDITOR_PORT and STUDYFORGE_SITE_PORT name one port: two services cannot both publish it"*. The site, editor and runner were **Created and never started** |
| **An out-of-range port (70000)** | `record_instance` and `studyforge serve` each refuse it by name: *"STUDYFORGE_SITE_PORT must be a whole port number from 1 to 65535"*. ⚠️ Under compose, compose itself refuses first, with `invalid containerPort: 70000`, before the preflight can run. That message quotes the value rather than the variable (`ISO-44/1`). Nothing started either way |
| **`up --wait` returns only once the site answers** (the good instance restored) | `up -d --wait` exited 0 after 2.7 s. The site container was **healthy**, having passed its first check, which fetches its own page. A `curl` right after `up` returned 200 |
| **The example panel names the missing service** | **Not readable on this corpus.** ISO links no code file from its lessons, so no built page carries a code example (`data-code-example` is on 0 of the 43 pages). The panel is W505's `code-example.html` |
| **A practice Run** (fundamentals unit 4, `Mti.java`) | Run gave "Finished.", exit 0. Submit gave "Passed.", "every check passed". Both went through the site to the runner's run service. The quiz still grades in the page, and the site logged 0 requests |

## Findings

| id | what |
|---|---|
| `ISO-44/1` | Low, framework or compose. A port outside 1–65535 in `instance.env` never reaches the preflight under compose. Compose refuses it at parse time with `invalid containerPort: 70000`, which quotes the value and does not name `STUDYFORGE_SITE_PORT`. `serve` and `record_instance` do name it. Nothing starts in any case |
| `ISO-44/2` | Low, framework. Step 5b's argv is a bare `docker build`. Nothing in it prevents a pull, and `docker build` has no `--pull never`. `--pull=false` was passed here by hand |
| — | ISO-43/1 (the ignored `site/.gitignore`), ISO-37/2, ISO-37/4 and ISO-36/2 still stand |

## For the register

- `main` was not moved. The corpus main checkout, :8770 and the running ISO containers were not
  touched. Nothing was pushed. Only this row's own pytest directories were deleted.
- **To switch over:**
  1. Advance the main checkout to this tip.
  2. Run `stage_site` there. It gives `…-0d0cfd0720c3`, which is already built on this host.
  3. Run "Bring it up" with the four env files. The site's tag moved, so the site is recreated;
     the editor and runner keep their tags.
