# ISO-42 handoff: the editor tag of toolchain `6e226d0` recorded

**Office** `po-int`. **Branch** `int/iso-42-execution`, from `main` `f1a576d` (ISO-41).
**Framework** the installed `studyforge` library at `5f17031a`, with no re-pin. **Toolchain**
`code-server-toolchain` at `6e226d0`, which carries W502: the editor's static path now carries a
digest of its product files.

| commit | what |
|---|---|
| `2b8cc16` | the execution regenerate, recording the new editor tag |
| this commit | this handoff |

## What was done

- The execution skill ran with `generate`, `write`, `record_runner` and `record_editor`, reading
  the toolchain's `consuming.json` from a `git archive` of `6e226d0`.
- The recorded tags are exactly the expected ones:
  - editor `code-server-toolchain/editor:java-maven-amd64-418405a27fd2`, which was
    `…-191d025af23b`;
  - runner `code-server-toolchain/runner:java-maven-amd64-57f9563fbd20`, unchanged.
- **No image was built.** `docker image inspect` found both tags already present.
- The diff is `editor.env` and `written.json`, and nothing else.

## Gates

On the host, with the installed library `5f17031a`, in this worktree.

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0, 0 findings |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0. The build changed no file, so the tree was clean |
| the corpus suite at `2b8cc16` | GREEN, exit 0, 39 passed. Its pytest run directory was then deleted |
| `hand_edited('.')` | `[]` |
| the compose `config --images` (runner, editor and instance env files; neither image variable set) | GREEN, exit 0. It named exactly `…/editor:java-maven-amd64-418405a27fd2` and `…/runner:java-maven-amd64-57f9563fbd20` |

## Findings

None new.

## For the register

- `main` was not moved. The corpus main checkout, :8770 and its containers were not touched.
  Nothing was pushed.
- **To redeploy:** advance the main checkout to this tip, and recreate the editor on
  `…-418405a27fd2`. The runner's tag did not move.
