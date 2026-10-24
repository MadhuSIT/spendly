#!/usr/bin/env python3
"""Validate SMS ML datasets for schema, class coverage, privacy hygiene, and leakage."""
from collections import Counter
from pathlib import Path
import re
from train_sms_model import CORPORA, EVALUATIONS, LABELS, OUTPUT, MAX_MODEL_SOURCE_BYTES, read_corpus

def normalized(text):
    return " ".join(text.lower().split())

def main():
    training = [row for path in CORPORA for row in read_corpus(path)]
    evaluation = [row for path in EVALUATIONS for row in read_corpus(path)]
    train_texts = {normalized(text) for _, text in training}
    eval_texts = {normalized(text) for _, text in evaluation}
    overlap = train_texts & eval_texts
    if overlap:
        raise SystemExit(f"Training/evaluation leakage: {len(overlap)} identical normalized messages")
    train_counts = Counter(label for label, _ in training)
    eval_counts = Counter(label for label, _ in evaluation)
    if set(train_counts) != LABELS or set(eval_counts) != LABELS:
        raise SystemExit("Training and evaluation must each cover all supported labels")
    if any(count < 4 for count in train_counts.values()):
        raise SystemExit("Each training label must have at least four examples")
    if any(count < 2 for count in eval_counts.values()):
        raise SystemExit("Each evaluation label must have at least two examples")
    field_text = "\n".join(path.read_text(encoding="utf-8") for path in [CORPORA[-1], EVALUATIONS[-1]])
    if re.search(r"(?<!\d)\d{10,}(?!\d)", field_text):
        raise SystemExit("Field regression corpus contains a long identifier; redact phone/reference/account identifiers")
    if "[REDACTED]" not in field_text:
        raise SystemExit("Field regression examples must use explicit redaction markers where references would appear")
    if OUTPUT.exists() and OUTPUT.stat().st_size > MAX_MODEL_SOURCE_BYTES:
        raise SystemExit(f"Generated Kotlin model exceeds {MAX_MODEL_SOURCE_BYTES} bytes")
    print(f"dataset_validation=PASS")
    print(f"training_samples={len(training)}")
    print(f"training_class_counts={dict(sorted(train_counts.items()))}")
    print(f"evaluation_samples={len(evaluation)}")
    print(f"evaluation_class_counts={dict(sorted(eval_counts.items()))}")
    print(f"exact_train_eval_overlap={len(overlap)}")
    print(f"model_source_bytes={OUTPUT.stat().st_size if OUTPUT.exists() else 'not generated yet'}")

if __name__ == "__main__":
    main()
