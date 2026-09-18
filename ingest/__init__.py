"""The iso-8583-jpos-tutorial adapter: read this source, write an archive, prove it.

**What it does.** Reads this repository's own material and writes the archive `studyforge validate` accepts. 1 container level(s), variant(s) prose, document kind(s) lesson — every one of those from `corpus.json`.

**How you use it.** `python3 -m ingest <corpus-root>` writes the archive and audits it. The exit code is the answer; there is no other agreement with the framework.

**Depends on.** `studyforge` as a sibling checkout, and nothing else. ⛔ The framework never imports this package: the seam is on disk (R2).
"""

from __future__ import annotations


from ingest.audit import audit
from ingest.emit import emit
from ingest.read import containers, documents

__all__ = ["audit", "containers", "documents", "emit"]
