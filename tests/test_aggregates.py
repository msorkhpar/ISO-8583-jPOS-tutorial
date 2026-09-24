"""The three whole-series aggregates are their parts, asserted by digest (ISO-31).

**What it does.** `corpus.json` excludes `src/ISO.md`, `src/Client.md` and `src/Server.md` as
"the exact ordered concatenation" of their parts, "asserted by digest". Until ISO-31 nothing
asserted it, and the fix rounds ISO-27 to ISO-29 let the aggregates drift. This test makes the
claim true: each aggregate's SHA-256 must equal the SHA-256 of its parts, joined as below.

**The rule, measured and not assumed.** Each part appears in its aggregate verbatim and in
order. Only runs of newlines separate them (the aggregate's own blank lines, which vary by
boundary), and a run of newlines ends the file. The runs below were read from the aggregates
at `1a31fbe`, the last commit before any part was edited. At that commit this rule rebuilds all
three byte for byte.

**How you use it.**

- `python3 -m pytest tests/test_aggregates.py` checks the claim.
- `python3 tests/test_aggregates.py --write` regenerates the three aggregates from their parts.
  It is the one way to bring them back in line after a part changes. ⛔ Never hand-edit an
  aggregate.

**Depends on.** The standard library and the parts on disk. Nothing from the framework.
"""

from __future__ import annotations

import difflib
import hashlib
import sys
from pathlib import Path

CORPUS_ROOT = Path(__file__).resolve().parents[1]

#: aggregate -> (its parts in order, the newline run before each part, the newline run at the end).
SERIES: dict[str, tuple[tuple[str, ...], tuple[int, ...], int]] = {
    "src/ISO.md": (
        tuple(f"src/{n}.md" for n in range(1, 17)),
        (0, 2, 2, 2, 2, 1, 2, 3, 2, 3, 3, 2, 2, 3, 3, 3),
        1,
    ),
    "src/Client.md": (
        tuple(f"src/c{n}.md" for n in range(1, 12)),
        (0, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2),
        0,
    ),
    "src/Server.md": (
        tuple(f"src/s{n}.md" for n in range(1, 12)),
        (0, 3, 4, 3, 3, 3, 3, 3, 2, 3, 3),
        0,
    ),
}


def assembled(root: Path, aggregate: str, parts_text: dict[str, bytes] | None = None) -> bytes:
    """Return what `aggregate` must hold: its parts, verbatim and in order, with its own runs."""
    parts, runs, tail = SERIES[aggregate]
    read = parts_text or {}
    return (
        b"".join(b"\n" * run + read.get(part, (root / part).read_bytes()) for run, part in zip(runs, parts))
        + b"\n" * tail
    )


def drift(root: Path, aggregate: str, parts_text: dict[str, bytes] | None = None) -> str:
    """Return '' when the digests agree, else a unified diff of the aggregate against its parts."""
    want = assembled(root, aggregate, parts_text)
    have = (root / aggregate).read_bytes()
    if hashlib.sha256(have).digest() == hashlib.sha256(want).digest():
        return ""
    return "".join(
        difflib.unified_diff(
            have.decode("utf-8").splitlines(keepends=True),
            want.decode("utf-8").splitlines(keepends=True),
            fromfile=f"{aggregate} (on disk)",
            tofile=f"{aggregate} (its parts)",
            n=1,
        )
    )


def test_every_aggregate_is_its_parts_by_digest():
    found = {aggregate: drift(CORPUS_ROOT, aggregate) for aggregate in SERIES}
    drifted = {aggregate: diff for aggregate, diff in found.items() if diff}
    assert not drifted, "an aggregate drifted from its parts; run tests/test_aggregates.py --write:\n" + "".join(
        drifted.values()
    )


def test_the_check_fires_on_a_one_character_drift():
    # ⭐ A check nobody has watched fire is a check nobody knows still works: change one
    # character of one part, in memory only, and the digest must disagree.
    parts, _, _ = SERIES["src/Client.md"]
    text = (CORPUS_ROOT / parts[2]).read_bytes()
    at = text.index(b"channel")
    planted = {parts[2]: text[:at] + b"C" + text[at + 1 :]}
    assert drift(CORPUS_ROOT, "src/Client.md", planted)


def test_the_parts_are_the_manifests_own_exclusions():
    # The manifest names exactly these three as aggregates, so a fourth one would need a
    # line here. This keeps the test and the manifest's claim from drifting apart.
    manifest = (CORPUS_ROOT / "corpus.json").read_text(encoding="utf-8")
    for aggregate in SERIES:
        assert f'"path": "{aggregate}"' in manifest
    assert manifest.count("whole-series aggregate") == len(SERIES)


if __name__ == "__main__":
    if sys.argv[1:] != ["--write"]:
        sys.exit("usage: python3 tests/test_aggregates.py --write")
    for name in SERIES:
        (CORPUS_ROOT / name).write_bytes(assembled(CORPUS_ROOT, name))
        print("wrote", name)
