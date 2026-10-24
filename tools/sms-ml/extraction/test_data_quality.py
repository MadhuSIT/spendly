#!/usr/bin/env python3
"""Fast data/weak-label regressions for the SMS extraction prototype."""
from __future__ import annotations

import unittest

from train_export import DATA, normalize_text, read_rows, weak_slots


class WeakLabelRegressionTests(unittest.TestCase):
    def test_negated_debit_is_not_a_positive_direction(self):
        text = "Payment failed for INR 560.00. Your account has not been debited."
        self.assertNotIn("DIRECTION", weak_slots(text, "FAILED"))

    def test_positive_debit_still_gets_direction_label(self):
        text = "INR 560.00 was debited from account XX1234."
        self.assertEqual(weak_slots(text, "TRANSACTION").get("DIRECTION"), "debited")

    def test_all_fixed_splits_have_valid_exact_spans_and_all_intents(self):
        from train_export import INTENTS

        for name in ("train", "validation", "test", "challenge"):
            rows = read_rows(DATA / f"{name}.jsonl")
            self.assertTrue(rows, f"{name} split must not be empty")
            self.assertEqual(set(INTENTS), {row["intent"] for row in rows})
            for row in rows:
                for value in row.get("slots", {}).values():
                    self.assertIn(value, row["text"])

    def test_challenge_is_disjoint_from_other_fixed_splits(self):
        splits = {
            name: {normalize_text(row["text"]) for row in read_rows(DATA / f"{name}.jsonl")}
            for name in ("train", "validation", "test", "challenge")
        }
        names = list(splits)
        for i, left in enumerate(names):
            for right in names[i + 1:]:
                self.assertFalse(
                    splits[left] & splits[right],
                    f"Exact text overlap between {left} and {right}",
                )


if __name__ == "__main__":
    unittest.main(verbosity=2)
