#!/usr/bin/env python3
"""Template/provider-family split tooling for the SMS extraction fixed splits.

Standalone and dependency-free (no torch/transformers) so it can run anywhere,
including on a restricted machine without the base-model download.

A "family" is the template skeleton of a message: every labelled slot value is
replaced by its field placeholder and every remaining digit run is collapsed.
Rows that share a skeleton came from the same template and MUST NOT be split
across train/validation/test — otherwise evaluation leaks and overstates quality.

Usage:
  python split_by_family.py --check        # exit 1 if families leak across splits
  python split_by_family.py --manifest     # write splits.json family manifest
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

DATA = Path(__file__).resolve().parent
FIXED_SPLITS = ("train", "validation", "test", "challenge")
# Splits that are meant to be mutually disjoint at the template-family level.
# challenge is a sealed holdout and must never share a family with the rest.
_DIGITS = re.compile(r"\d+")
_WS = re.compile(r"\s+")


def load_rows(name: str) -> list[dict]:
    path = DATA / f"{name}.jsonl"
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip():
            rows.append(json.loads(line))
    return rows


def family_skeleton(row: dict) -> str:
    """Return the normalized template skeleton for a labelled row."""
    text = row["text"]
    # Replace longer slot values first so short values don't corrupt longer spans.
    slots = sorted(
        row.get("slots", {}).items(), key=lambda kv: len(kv[1]), reverse=True
    )
    for field, value in slots:
        if value:
            text = text.replace(value, f"<{field}>")
    text = text.lower()
    text = _DIGITS.sub("<n>", text)        # collapse any leftover raw digits
    text = _WS.sub(" ", text).strip()
    return text


def family_key(row: dict) -> str:
    return hashlib.sha1(family_skeleton(row).encode("utf-8")).hexdigest()[:12]


def families_by_split() -> dict[str, dict[str, list[str]]]:
    """Map split -> family_key -> list of message texts in that family."""
    out: dict[str, dict[str, list[str]]] = {}
    for name in FIXED_SPLITS:
        buckets: dict[str, list[str]] = {}
        for row in load_rows(name):
            buckets.setdefault(family_key(row), []).append(row["text"])
        out[name] = buckets
    return out


def find_leakage(by_split: dict[str, dict[str, list[str]]]) -> list[dict]:
    """Return cross-split family collisions (same template in >1 split)."""
    owners: dict[str, list[str]] = {}
    for split, buckets in by_split.items():
        for fam in buckets:
            owners.setdefault(fam, []).append(split)
    leaks = []
    for fam, splits in owners.items():
        if len(splits) > 1:
            example = next(iter(by_split[splits[0]][fam]))
            leaks.append({"family": fam, "splits": sorted(splits), "example": example})
    return leaks


def build_manifest(by_split: dict[str, dict[str, list[str]]]) -> dict:
    leaks = find_leakage(by_split)
    return {
        "note": "Template-family distribution of the fixed splits. "
                "Families appearing in >1 split indicate evaluation leakage.",
        "splits": {
            name: {
                "rows": sum(len(v) for v in buckets.values()),
                "families": len(buckets),
            }
            for name, buckets in by_split.items()
        },
        "cross_split_family_leaks": leaks,
        "leak_count": len(leaks),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true",
                        help="exit non-zero if any template family spans multiple splits")
    parser.add_argument("--manifest", action="store_true",
                        help="write splits.json family manifest next to the data")
    args = parser.parse_args()

    by_split = families_by_split()
    manifest = build_manifest(by_split)

    if args.manifest:
        out = DATA / "splits.json"
        out.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        print(f"wrote {out}")

    for name, info in manifest["splits"].items():
        print(f"{name:11s} rows={info['rows']:4d} families={info['families']:4d}")
    print(f"cross_split_family_leaks={manifest['leak_count']}")
    for leak in manifest["cross_split_family_leaks"]:
        print(f"  LEAK {leak['family']} in {leak['splits']}: {leak['example'][:70]!r}")

    if args.check and manifest["leak_count"]:
        print("FAIL: template families leak across splits", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
