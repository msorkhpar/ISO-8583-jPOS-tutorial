"""The authored exercises: committed bundles, emitted as practice documents (ISO-21).

**What it does.** Finds every bundle the authoring pass committed under
`exercises/`, refuses one the gates did not clear, and answers with two things
`ingest.read` needs: how many practices each unit now carries, and the fields of
each authored practice document, in the shape `ingest.emit` builds from.

**How you use it.** `authored(root)` once per run, then `counted(...)` from
`read.containers` and `documents(...)` from `read.documents`. Nothing else calls
this module.

**Depends on.** `studyforge.exercise.bundle` for the bundle, its places and its
emission; `studyforge.exercise.gates` for the gate record; `studyforge.archive`
for `build` and the personal-data gate; `studyforge.corpus.manifest` for the
source name. ⛔ Nothing here authors, and nothing here writes a file.

## ⛔ The document only, never the workspace (`AX-10/4`)

⭐ The authoring pass already created each exercise's workspace under
`practice/`. So this module uses `emission.document` and nothing else. The
bundle package's `write` would refuse those files as already there (R3), and
that refusal is correct: the files are the pass's and not this adapter's.

## ⛔ Authored practices number AFTER the source's own (`W437`)

⭐ A unit that carries the source's practice (`iso-fundamentals` units 2, 3 and
4 carry `practice-1`) gets its authored exercises from `practice-2` on. The
pass numbers them that way. This module checks the numbering rather than
trusting it: a unit's authored ordinals must run from one past its archived
practices with no gap, and then its `practices` count is raised by exactly that
many. ⚠️ Otherwise `validate`'s practice-count check would refuse the container
map, and a gap would mean a lost exercise.

## ⛔ A bundle the gates did not clear is refused HERE, before any document

⭐ `studyforge validate` refuses an ungated `generated` exercise too. ⛔ But an
adapter that emitted one and left the refusal to a later step would have
written an archive that holds it. So every bundle's gate record must be present,
must clear, and must still digest to the files beside it, or the whole run
raises, naming the bundle's directory. Nothing half-emitted reaches the archive
either way: `ingest.emit` stages the whole archive and moves it only at the end.
"""

from __future__ import annotations

import dataclasses
import json
from dataclasses import dataclass
from pathlib import Path

from studyforge.address import Address
from studyforge.archive.document import build
from studyforge.archive.scrub import assert_clean
from studyforge.corpus.manifest import MANIFEST_FILENAME
from studyforge.corpus.manifest import load as load_manifest
from studyforge.exercise import from_document
from studyforge.exercise.bundle import (
    BUNDLE_FILENAME,
    BUNDLES_DIRNAME,
    Bundle,
    Places,
    bundle_of,
    emit,
)
from studyforge.exercise.gates import drifted, record_of
from studyforge.skills.exercises import QUIZ_API, QUIZ_DOCUMENT, QUIZ_KEYS

#: ⚠️ The date an emission is taken with. `ingest.emit` rebuilds every document
#: with the run's own `ingested` (INT-09/3), so this value never reaches the
#: archive, and a date recorded here would be a clock in the reader (R10).
PLACEHOLDER_DATE = "1970-01-01"

#: The fields of an emitted document that `archive.document.build` takes as
#: input. ⭐ Everything else in the document (its counts, its digest, its API
#: version) is `build`'s arithmetic over these, and is recomputed.
BUILD_FIELDS = ("variant", "unit", "kind", "ordinal", "title", "blocks", "starting_code", "exercise")


#: ⭐ What a quiz practice shows above its questions (ISO-25). The framework has
#: no `emit` for a quiz (its guide says the adapter builds the document from
#: `tests/quiz.json`), so these two blocks are this adapter's. ⚠️ They say
#: nothing about where the answer is checked: that is the framework's (W451).
QUIZ_BLOCKS = (
    {"type": "heading", "level": 2, "text": "Check yourself"},
    {"type": "para", "text": "Questions on what this page has just taught. Choose one answer for each."},
)


@dataclass(frozen=True, slots=True)
class Quiz:
    """One committed quiz bundle: its identity, its title and its exercise record.

    ⭐ It answers `places` and `ordinal` as a `Bundle` does, so `counted` and the
    gate-record check read both shapes the same way.
    """

    address: Address
    variant: str
    unit: int
    ordinal: int
    title: str
    exercise: dict
    places: Places


class BundleRefused(RuntimeError):
    """A committed bundle this adapter will not emit, and why.

    ⛔ Names the bundle's corpus-relative directory and the rule, never a
    machine path or a value (R7).
    """


def authored(root: Path | str) -> dict[tuple[str, str, int], tuple[Bundle, ...]]:
    """Return every committed bundle, cleared by its gates, grouped by page.

    The key is `(address key, variant, unit)`, and each page's bundles are
    sorted by ordinal. ⛔ **Every bundle is checked before any is returned**,
    so one bad bundle refuses the run rather than being skipped (R6).
    """
    base = Path(root)
    found: dict[tuple[str, str, int], list[Bundle]] = {}
    for path in sorted((base / BUNDLES_DIRNAME).rglob(BUNDLE_FILENAME)):
        where = path.parent.relative_to(base).as_posix()
        bundle = bundle_of(json.loads(path.read_text(encoding="utf-8")), f"{where}/{BUNDLE_FILENAME}")
        if bundle.places.bundle != where:
            raise BundleRefused(
                f"the bundle at '{where}' declares an identity whose directory is "
                f"'{bundle.places.bundle}'. A bundle's directory is derived from what it "
                f"declares, so one that sits elsewhere is refused rather than emitted "
                f"under an identity nobody can find it by."
            )
        _require_cleared(base, bundle, where)
        key = (bundle.address.key, bundle.variant, bundle.unit)
        found.setdefault(key, []).append(bundle)
    for path in sorted((base / BUNDLES_DIRNAME).rglob(Path(QUIZ_DOCUMENT).name)):
        where = path.parent.parent.relative_to(base).as_posix()
        if path.relative_to(base).as_posix() != f"{where}/{QUIZ_DOCUMENT}":
            continue
        quiz = _quiz_of(path, where)
        _require_cleared(base, quiz, where)
        key = (quiz.address.key, quiz.variant, quiz.unit)
        found.setdefault(key, []).append(quiz)
    return {key: tuple(sorted(page, key=lambda one: one.ordinal)) for key, page in found.items()}


def counted(bundles, container) -> tuple:
    """Return `container.units` with each unit's `practices` raised by its authored ones.

    ⛔ **The authored ordinals must continue the archived ones with no gap**
    (`W437`), and a bundle addressing a unit this container does not declare is
    refused rather than dropped.
    """
    declared = {unit.n for unit in container.units}
    for (address, variant, number), page in bundles.items():
        if address == container.address.key and variant == container.variant:
            if number not in declared:
                raise BundleRefused(
                    f"'{page[0].places.bundle}' is an exercise for unit {number} of "
                    f"'{address}', which declares {len(declared)} unit(s). An exercise "
                    f"joins a unit the curriculum records; remove the bundle or correct it."
                )
    units = []
    for unit in container.units:
        page = bundles.get((container.address.key, container.variant, unit.n), ())
        wanted = list(range(unit.practices + 1, unit.practices + len(page) + 1))
        if [one.ordinal for one in page] != wanted:
            raise BundleRefused(
                f"unit {unit.n} of '{container.address.key}' carries {unit.practices} "
                f"archived practice(s), so its authored exercises must be numbered "
                f"{wanted}, and the committed bundles are numbered "
                f"{[one.ordinal for one in page]}. A gap is a lost exercise and a "
                f"collision would overwrite the source's own practice (W437)."
            )
        units.append(dataclasses.replace(unit, practices=unit.practices + len(page)))
    return tuple(units)


def documents(root: Path | str, bundles, container, unit: int) -> list[dict]:
    """Return the fields of each authored practice document on one unit's page.

    ⭐ **The fields are read back off the framework's own emission**, and then
    checked: rebuilt with the same inputs, they must give the emission's
    document byte for byte. So a field this adapter failed to carry refuses the
    run here instead of disappearing from the archive.
    """
    base = Path(root)
    page = bundles.get((container.address.key, container.variant, unit), ())
    if not page:
        return []
    source = load_manifest(base / MANIFEST_FILENAME).source
    fields = []
    for bundle in page:
        if isinstance(bundle, Quiz):
            fields.append(_quiz_fields(bundle, source))
            continue
        emission = emit(base, bundle, source=source, ingested=PLACEHOLDER_DATE)
        document = emission.document
        taken = {"address": bundle.address, **{key: document[key] for key in BUILD_FIELDS}}
        if build(source=source, ingested=PLACEHOLDER_DATE, **taken) != document:
            raise BundleRefused(
                f"'{bundle.places.bundle}' emits a document this adapter cannot carry "
                f"whole: rebuilt from {list(BUILD_FIELDS)} it differs from the "
                f"framework's emission. A field the emission gained is missing here."
            )
        fields.append(taken)
    return fields


def _quiz_of(path: Path, where: str) -> Quiz:
    """Read one quiz's own document, refusing one whose identity or record will not read."""
    document = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(document, dict) or tuple(document) != QUIZ_KEYS:
        raise BundleRefused(
            f"the quiz at '{where}' carries a document that is not {list(QUIZ_KEYS)} in "
            f"that order, so it was not written by the authoring pass this adapter reads."
        )
    if document["quiz_api"] != QUIZ_API:
        raise BundleRefused(
            f"the quiz at '{where}' is written at a quiz_api this adapter does not read "
            f"(it reads {QUIZ_API})."
        )
    address = Address.of(*document["address"])
    places = Places(address, document["variant"], document["unit"], document["ordinal"])
    if places.bundle != where:
        raise BundleRefused(
            f"the quiz at '{where}' declares an identity whose directory is "
            f"'{places.bundle}'. It is refused rather than emitted under an identity "
            f"nobody can find it by."
        )
    exercise = from_document(document["exercise"], f"{where}/{QUIZ_DOCUMENT}")
    if not exercise.is_quiz:
        raise BundleRefused(f"the document at '{where}/{QUIZ_DOCUMENT}' is not a quiz.")
    return Quiz(
        address,
        document["variant"],
        document["unit"],
        document["ordinal"],
        document["title"],
        document["exercise"],
        places,
    )


def _quiz_fields(quiz: Quiz, source: str) -> dict:
    """Return one quiz practice's fields, checked by building the document once."""
    taken = {
        "address": quiz.address,
        "variant": quiz.variant,
        "unit": quiz.unit,
        "kind": "practice",
        "ordinal": quiz.ordinal,
        "title": quiz.title,
        "blocks": [dict(block) for block in QUIZ_BLOCKS],
        "exercise": quiz.exercise,
    }
    build(source=source, ingested=PLACEHOLDER_DATE, **taken)
    return taken


def _require_cleared(base: Path, bundle: Bundle | Quiz, where: str) -> None:
    """Refuse a bundle whose gate record is absent, did not clear, or no longer matches."""
    gates = base / bundle.places.gates
    if not gates.is_file():
        raise BundleRefused(
            f"'{where}' ships no gate record. An authored exercise ships only with the "
            f"record of the gates it cleared (spec §7), so this one was never proven "
            f"and is refused rather than emitted."
        )
    decoded = json.loads(gates.read_text(encoding="utf-8"))
    assert_clean(decoded, bundle.places.gates)
    record = record_of(decoded, bundle.places.gates)
    if not record.clears:
        raise BundleRefused(
            f"'{where}' ships a gate record in which not every gate held. A shortfall "
            f"is reported, never emitted (R6): re-author the exercise."
        )
    moved = drifted(base / bundle.places.bundle, record.inputs, bundle.places.bundle)
    if moved:
        raise BundleRefused(
            f"'{where}' no longer matches the gate record beside it: {moved[0]} The "
            f"gates were run over other files, so what they proved is not this bundle."
        )
