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
from pathlib import Path

from studyforge.archive.document import build
from studyforge.archive.scrub import assert_clean
from studyforge.corpus.manifest import MANIFEST_FILENAME
from studyforge.corpus.manifest import load as load_manifest
from studyforge.exercise.bundle import BUNDLE_FILENAME, BUNDLES_DIRNAME, Bundle, bundle_of, emit
from studyforge.exercise.gates import drifted, record_of

#: ⚠️ The date an emission is taken with. `ingest.emit` rebuilds every document
#: with the run's own `ingested` (INT-09/3), so this value never reaches the
#: archive, and a date recorded here would be a clock in the reader (R10).
PLACEHOLDER_DATE = "1970-01-01"

#: The fields of an emitted document that `archive.document.build` takes as
#: input. ⭐ Everything else in the document (its counts, its digest, its API
#: version) is `build`'s arithmetic over these, and is recomputed.
BUILD_FIELDS = ("variant", "unit", "kind", "ordinal", "title", "blocks", "starting_code", "exercise")


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


def _require_cleared(base: Path, bundle: Bundle, where: str) -> None:
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
