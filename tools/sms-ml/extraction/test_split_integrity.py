#!/usr/bin/env python3
"""Torch-free integrity guards for the SMS extraction fixed splits.

Runs without torch/transformers so it can gate commits on any machine
(including a restricted one without the base-model download).

Guards:
  * the sealed `challenge` holdout shares no template family with train/val/test
  * no raw PII (card/account/phone/OTP/email) survives in any fixed split
  * every labelled slot value is an exact substring of its message
"""
from __future__ import annotations

import unittest

import scrub
from split_by_family import FIXED_SPLITS, family_key, load_rows


class SplitIntegrityTests(unittest.TestCase):
    def test_challenge_is_family_disjoint_from_other_splits(self):
        challenge = {family_key(r) for r in load_rows("challenge")}
        others = set()
        for name in ("train", "validation", "test"):
            others |= {family_key(r) for r in load_rows(name)}
        leaked = challenge & others
        self.assertFalse(
            leaked,
            f"challenge shares {len(leaked)} template family(ies) with train/val/test",
        )

    def test_no_raw_pii_in_fixed_splits(self):
        offenders = []
        for name in FIXED_SPLITS:
            for i, row in enumerate(load_rows(name), 1):
                hits = scrub.findings(row["text"])
                if hits:
                    offenders.append(f"{name}.jsonl:{i}: {hits}")
        self.assertFalse(offenders, "raw PII present:\n" + "\n".join(offenders))

    def test_slot_values_are_exact_substrings(self):
        for name in FIXED_SPLITS:
            for i, row in enumerate(load_rows(name), 1):
                for field, value in row.get("slots", {}).items():
                    self.assertTrue(value, f"{name}.jsonl:{i}: empty {field}")
                    self.assertIn(
                        value, row["text"],
                        f"{name}.jsonl:{i}: {field}={value!r} not an exact substring",
                    )


if __name__ == "__main__":
    unittest.main(verbosity=2)
