# ISO-41 handoff: the tags of toolchain `395fffe` recorded

**Office** `po-int`. **Branch** `int/iso-41-execution`, from `main` `473017e` (ISO-40).
**Framework** the installed `studyforge` library at `5f17031a`, with no re-pin. **Toolchain**
`code-server-toolchain` at `395fffe`, which carries W501 (the editor's built-in chat removed) and
W492's toolchain half.

| commit | what |
|---|---|
| `b8af073` | the execution regenerate, recording both new tags |
| this commit | this handoff |

## What was done

- The execution skill ran with `generate`, `write`, `record_runner` and `record_editor`, reading
  the toolchain's `consuming.json` from a `git archive` of `395fffe`.
- The recorded tags are exactly the expected ones:
  - runner `code-server-toolchain/runner:java-maven-amd64-57f9563fbd20`, which was
    `…-230dc78178c2`;
  - editor `code-server-toolchain/editor:java-maven-amd64-191d025af23b`, which was
    `…-415f4b759fae`.
- **No image was built.** `docker image inspect` found both tags already present, since the W501
  office built them.
- The diff is `runner.env`, `editor.env` and `written.json`, and nothing else. `compose.yaml`,
  `EXECUTION.md`, `toolchain.json` and the prime rewrote byte-identical.

## Gates

On the host, with the installed library `5f17031a`, in this worktree.

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0, 0 findings |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0. The build changed no file, so the tree was clean |
| the corpus suite at `b8af073` | GREEN, exit 0, 39 passed. Its pytest run directory was then deleted |
| `hand_edited('.')` | `[]` |
| the compose `config --images` (runner, editor and instance env files; neither image variable set) | GREEN, exit 0. It named exactly `…/runner:java-maven-amd64-57f9563fbd20` and `…/editor:java-maven-amd64-191d025af23b` |

## Findings

None new. ISO-40/1 (the chat side bar) should be closed by W501 once the site's editor is recreated
on `…-191d025af23b`. This row did not start that image to look.

## For the register

- `main` was not moved. The corpus main checkout, :8770 and its containers were not touched.
  Nothing was pushed.
- **To redeploy:** advance the main checkout to this tip. Then recreate **both** containers with
  "Bring it up" in `EXECUTION.md`. Both images are already on this host.
