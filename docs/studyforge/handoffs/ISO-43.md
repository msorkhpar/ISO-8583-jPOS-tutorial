# ISO-43 handoff: one compose publishes the course

**Office** `po-int`. **Branch** `int/iso-43-publish`, from `main` `988955e` (ISO-42).
**Framework** `studyforge` 0.1.0, built from `f5b5a950` (W503 and W504 merged). **Toolchain**
`code-server-toolchain` at `6e226d0`, unchanged. **narrate-service** at `62725dd`.

| commit | what |
|---|---|
| `38ea5f7` | re-onboard, the three-service execution regenerate, the instance's ports, the staged site, the clip signal removed, and the build |
| this commit | this handoff |

## What was done

1. **Install.** `studyforge` was cloned into scratch at `f5b5a950`, and a wheel built from the
   clone was installed into a fresh venv. `PYTHONPATH` was unset. `library.commit()` reported
   `f5b5a95014c42c5c5a4795e40953cbe7696de834`. Then
   `reonboard(…, framework_commit=…).write(…, regenerate=True)` ran. `hand_edited` read `[]`
   before and after.
2. **The execution regenerate** ran through the skill against a `git archive` of `6e226d0`.
   - `compose.yaml` now has three services. `site` runs `serve /corpus --published` on the
     instance's site port. The runner sits on an internal network (`runs`) with a run service.
   - New files:
     - `.studyforge/execution/runservice.pl`;
     - `allowed/.gitignore`;
     - `site.env` (`STUDYFORGE_SITE_IMAGE`, `COMPOSE_PROFILES=site`);
     - `site/site.containerfile`.
   - `EXECUTION.md` follows these changes.
   - ⭐ **The tags recorded:**
     - runner `code-server-toolchain/runner:java-maven-amd64-57f9563fbd20`, unchanged;
     - editor `code-server-toolchain/editor:java-maven-amd64-418405a27fd2`, unchanged;
     - site `studyforge-site:0.1.0-e173e33edf3f`, new.
3. **The ports, through `record_instance`:** `port=8443`, `site_port=8770`. `instance.env` gains
   `STUDYFORGE_SITE_PORT=8770` and `STUDYFORGE_SITE_NAME=studyforge-site-iso-8583-jpos-tutorial`.
   The compose was **not** brought up on these ports.
4. **Step 5b.**
   - `stage_site('.')` checked that the installed library is the pinned one, by version and
     commit. It copied the library into `site/library/`, which is ignored, and wrote the build
     file on `python:3.14-slim` pinned by digest.
   - Its `docker build` argv was run under the gate lock, with the base already present locally.
     It built `studyforge-site:0.1.0-e173e33edf3f`.
5. **W503.** `.studyforge/assets/narration-clips.js` was deleted, and the deletion is committed.
   The rebuilt pages no longer link it.
6. **Ingest** changed no archive byte. Then `build . --out .` ran.

## Gates

On the host, installed library `f5b5a950`, in this worktree.

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m pytest tests/test_framework_pin.py` | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0, 0 findings |
| `python3 -m studyforge.cli plan .` | GREEN, exit 0 |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0. After the commit, a second build left the tree clean |
| the corpus suite, before the commit | GREEN, exit 0, 39 passed |
| the corpus suite at `38ea5f7` | GREEN, exit 0, 39 passed |
| `hand_edited('.')` | `[]` |
| the compose `config --images` (runner, editor, instance and site env files; no image variable or profile in the environment) | GREEN, exit 0. It named exactly `…/runner:…-57f9563fbd20`, `…/editor:…-418405a27fd2` and `studyforge-site:0.1.0-e173e33edf3f` |

## The live reading, on a scratch compose

- **The setup.** A `git archive` of `38ea5f7` went into scratch. `record_instance` gave it project
  `iso43-live`, editor port 18443 and site port 18793, with containers `iso43-live-editor`,
  `iso43-live-runner` and `iso43-live-site`. Port 18772 was taken by something else.
- It was brought up with EXECUTION.md's one command and `--pull never`. All three services were
  healthy.
- The pages were driven by headless Chrome over CDP.
- Afterwards the compose was brought down with `down -v`, and the copy was deleted. The live site's two
  containers were not touched: `docker events` shows no activity on them.

| reading | result |
|---|---|
| **Narration plays** (fundamentals unit 2) | The player shows. After "Play narration", the `<audio>` plays `iso-fundamentals--unit-02.prose.b1-cdf43215.mp3`: not paused, at 3.9 s after 4 s, with no media error. The clip was served with status 206 |
| **The quiz grades in the page** (fundamentals unit 1) | Wrong answers read "0 of 2 answered correctly." Right answers read "Every question answered correctly." **The site container's request log gained 0 lines** across both checks |
| **Run and Submit** (fundamentals unit 4, `Mti.java`) | Run gave "Finished.", exit 0. Submit gave "Passed.", "every check passed". Both went through the site's `POST /api/v1/run/…` to the runner's run service, with no `docker exec` |
| **With the runner stopped** (`docker stop iso43-live-runner`, then the page reloaded) | Run, Submit and Stop are hidden, and so is their controls row. The page says: *"This course's runner is not running here, so this page cannot run your code, and nothing runs it anywhere else. Start it, and the editor beside it, with the one command under "Bring it up" in this corpus's EXECUTION.md, run from the corpus's root, and reload this page."* |

⭐ In the same workspace screenshot, the editor on `…-418405a27fd2` shows **no chat side bar**.
**ISO-40/1 is closed** by W501.

## Findings

| id | what |
|---|---|
| `ISO-43/1` | Low, framework. `stage_site` writes `.studyforge/execution/site/.gitignore` (`/library/`). The build's own untracked `.studyforge/.gitignore` ignores every `.gitignore` under `.studyforge`, so this file is ignored and never committed. `allowed/.gitignore` avoids that with `!/.gitignore`, and `site/.gitignore` does not. Nothing is lost on a host where `stage_site` runs, because it rewrites the file. But a clone that stages the site before a build has no rule to ignore `site/library/`, so the copied library would show as untracked |
| — | ISO-40/2 (the GPU narration profile) was not exercised here, since nothing was narrated. ISO-37/2, ISO-37/4 and ISO-36/2 still stand |

## For the register

- `main` was not moved. The corpus main checkout, :8770 and the live site's containers were not
  touched. Nothing was pushed. Only this row's own pytest directories were deleted.
- **To publish on this tip:**
  1. Advance the main checkout.
  2. Stop the host serve that holds 8770.
  3. Run `stage_site` there. Its library copy is ignored and not committed, and the image
     `studyforge-site:0.1.0-e173e33edf3f` is already built on this host.
  4. Run "Bring it up" in `EXECUTION.md` with all four env files. The site answers on
     127.0.0.1:8770 and the editor on 8443.
