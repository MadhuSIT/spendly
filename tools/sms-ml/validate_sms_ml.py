#!/usr/bin/env python3
"""Validate Spendly's fixed-format SMS corpora and split hygiene."""
from collections import Counter
from pathlib import Path
import re
from train_sms_model import CORPORA, EVALUATIONS, LABELS, TEST_DATASET, OUTPUT, MAX_MODEL_SOURCE_BYTES, read_corpus

def normalized(text):
    return " ".join(text.lower().split())

def main():
    training = [row for path in CORPORA for row in read_corpus(path)]
    validation = [row for path in EVALUATIONS for row in read_corpus(path)]
    test = read_corpus(TEST_DATASET)
    train_texts = {normalized(text) for _, text in training}
    val_texts = {normalized(text) for _, text in validation}
    test_texts = {normalized(text) for _, text in test}
    if train_texts & val_texts or train_texts & test_texts or val_texts & test_texts:
        raise SystemExit("Exact normalized message leakage across train/validation/test splits")
    train_counts = Counter(label for label, _ in training)
    val_counts = Counter(label for label, _ in validation)
    test_counts = Counter(label for label, _ in test)
    for name, counts in (("training", train_counts), ("validation", val_counts), ("test", test_counts)):
        if set(counts) != LABELS:
            raise SystemExit(f"{name} must cover every supported class; missing={sorted(LABELS - set(counts))}")
    if min(train_counts.values()) < 4 or min(val_counts.values()) < 2 or min(test_counts.values()) < 2:
        raise SystemExit("At least four train and two validation/test examples per class are required")
    ROOT = Path(__file__).resolve().parents[2]
    field_text = "\n".join(path.read_text(encoding="utf-8") for path in [ROOT / "tools/sms-ml/field_regressions.tsv", ROOT / "tools/sms-ml/field_evaluation.tsv"])
    if re.search(r"(?<!\d)\d{10,}(?!\d)", field_text):
        raise SystemExit("A field corpus contains a long identifier; redact phone/reference/account identifiers")
    if "[REDACTED]" not in field_text:
        raise SystemExit("Field examples must use explicit redaction markers where needed")
    if OUTPUT.exists() and OUTPUT.stat().st_size > MAX_MODEL_SOURCE_BYTES:
        raise SystemExit(f"Generated model exceeds {MAX_MODEL_SOURCE_BYTES} bytes")
    print("dataset_validation=PASS")
    print(f"training_samples={len(training)}")
    print("training_class_counts=" + str(dict(sorted(train_counts.items()))))
    print(f"validation_samples={len(validation)}")
    print("validation_class_counts=" + str(dict(sorted(val_counts.items()))))
    print(f"test_samples={len(test)}")
    print("test_class_counts=" + str(dict(sorted(test_counts.items()))))
    print(f"exact_train_validation_test_overlap={len((train_texts & val_texts) | (train_texts & test_texts) | (val_texts & test_texts))}")
    print(f"model_source_bytes={OUTPUT.stat().st_size if OUTPUT.exists() else 'not generated yet'}")

if __name__ == "__main__":
    main()
