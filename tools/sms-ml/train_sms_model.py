#!/usr/bin/env python3
"""Train Spendly's compact offline SMS classifier using only Python stdlib.

Usage: python3 tools/sms-ml/train_sms_model.py
The generated Kotlin model is deterministic. Only reviewed, sanitized examples
from the checked-in TSV corpora are read; no inbox or network access is used.
"""
from collections import Counter, defaultdict
import math
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
CORPORA = [
    ROOT / "tools/sms-ml/training_corpus.tsv",
    ROOT / "tools/sms-ml/linked_account_scenarios.tsv",
    ROOT / "tools/sms-ml/field_regressions.tsv",
    ROOT / "tools/sms-ml/synthetic_training.tsv",
]
EVALUATIONS = [
    ROOT / "tools/sms-ml/evaluation_corpus.tsv",
    ROOT / "tools/sms-ml/field_evaluation.tsv",
    ROOT / "tools/sms-ml/synthetic_validation.tsv",
]
TEST_DATASET = ROOT / "tools/sms-ml/synthetic_test.tsv"
OUTPUT = ROOT / "app/src/main/java/com/madhusit/spendly/domain/sms/SmsMlModelData.kt"
VERSION = "sms-nb-2026-10-24-v5"
ALPHA = 0.5
TOP_FEATURES = 80
MIN_EVALUATION_ACCURACY = 0.80
MIN_TRANSACTION_RECALL = 0.70
MAX_MODEL_SOURCE_BYTES = 25 * 1024
LABELS = {
    "TRANSACTION", "AMBIGUOUS", "BALANCE", "FAILED", "MARKETING",
    "OTHER", "OTP", "REMINDER", "SECURITY_ALERT",
}

def tokenize(text):
    return re.findall(r"[a-z0-9]+", text.lower())

def features(text):
    # Unigrams generalize better across provider-specific word order and keep the model compact.
    return set(tokenize(text))

def kotlin_string(value):
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'

def read_corpus(path):
    samples = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        label, separator, text = line.partition("\t")
        if not separator or not label or not text.strip():
            raise SystemExit(f"Invalid corpus row at {path}:{line_number}; expected LABEL<TAB>TEXT")
        if label not in LABELS:
            raise SystemExit(f"Unknown label {label!r} at {path}:{line_number}")
        samples.append((label, text.strip()))
    return samples

def train(samples):
    labels = sorted({label for label, _ in samples})
    if labels != sorted(LABELS):
        raise SystemExit(f"Training labels mismatch; expected {sorted(LABELS)}, got {labels}")
    class_counts = {label: Counter() for label in labels}
    vocabulary = set()
    for label, text in samples:
        row_features = {token for token in features(text) if not any(char.isdigit() for char in token)}
        class_counts[label].update(row_features)
        vocabulary.update(row_features)
    vocabulary = sorted(vocabulary)
    vocab_size = len(vocabulary)
    totals = {label: sum(class_counts[label].values()) for label in labels}
    total_samples = len(samples)
    weights = {}
    biases = {}
    for label in labels:
        other = Counter()
        for other_label, counts in class_counts.items():
            if other_label != label:
                other.update(counts)
        own_total = totals[label]
        other_total = sum(other.values())
        token_weights = {}
        for token in vocabulary:
            own_probability = (class_counts[label][token] + ALPHA) / (own_total + ALPHA * vocab_size)
            other_probability = (other[token] + ALPHA) / (other_total + ALPHA * vocab_size)
            token_weights[token] = math.log(own_probability / other_probability)
        weights[label] = dict(sorted(
            token_weights.items(),
            key=lambda item: (-round(abs(item[1]), 10), item[0])
        )[:TOP_FEATURES])
        own_prior = sum(1 for sample_label, _ in samples if sample_label == label) / total_samples
        biases[label] = math.log(own_prior / (1.0 - own_prior))
    return labels, weights, biases

def predict(text, labels, weights, biases):
    row_features = features(text)
    logits = {
        label: biases[label] + sum(weights[label].get(token, 0.0) for token in row_features)
        for label in labels
    }
    maximum = max(logits.values())
    exponentials = {label: math.exp(max(-60.0, score - maximum)) for label, score in logits.items()}
    denominator = sum(exponentials.values()) or 1.0
    probabilities = {label: value / denominator for label, value in exponentials.items()}
    predicted = max(probabilities, key=probabilities.get)
    confidence = probabilities[predicted]
    if (predicted == "TRANSACTION" and confidence < 0.72) or predicted == "AMBIGUOUS" or confidence < 0.52:
        return "AMBIGUOUS"
    return predicted

def main():
    samples = [sample for path in CORPORA for sample in read_corpus(path)]
    counts = Counter(label for label, _ in samples)
    missing = sorted(LABELS - set(counts))
    too_small = {label: count for label, count in counts.items() if count < 4}
    if missing or too_small:
        raise SystemExit(f"Insufficient training coverage: missing={missing}, classes_below_4={too_small}")

    labels, weights, biases = train(samples)
    lines = [
        "package com.madhusit.spendly.domain.sms",
        "",
        "/** Generated by tools/sms-ml/train_sms_model.py; do not edit by hand. */",
        "internal object SmsMlModelData {",
        f"    const val VERSION = {kotlin_string(VERSION)}",
        "    val biases: Map<String, Double> = mapOf("
    ]
    for index, label in enumerate(labels):
        comma = "," if index < len(labels) - 1 else ""
        lines.append(f"        {kotlin_string(label)} to {biases[label]:.5f}{comma}")
    lines += ["    )", "    val weights: Map<String, Map<String, Double>> = mapOf("]
    for label_index, label in enumerate(labels):
        lines.append(f"        {kotlin_string(label)} to mapOf(")
        entries = list(weights[label].items())
        for index, (token, weight) in enumerate(entries):
            comma = "," if index < len(entries) - 1 else ""
            lines.append(f"            {kotlin_string(token)} to {weight:.5f}{comma}")
        suffix = "," if label_index < len(labels) - 1 else ""
        lines.append(f"        ){suffix}")
    lines += ["    )", "}", ""]
    generated = "\n".join(lines)
    OUTPUT.write_text(generated, encoding="utf-8")

    evaluation = [sample for path in EVALUATIONS for sample in read_corpus(path)]
    evaluation_labels = {label for label, _ in evaluation}
    if evaluation_labels != LABELS:
        raise SystemExit(f"Evaluation must cover all classes; missing={sorted(LABELS - evaluation_labels)}")
    predictions = [(expected, predict(text, labels, weights, biases)) for expected, text in evaluation]
    correct = sum(expected == actual for expected, actual in predictions)
    accuracy = correct / max(1, len(evaluation))
    false_positive_transactions = sum(expected != "TRANSACTION" and actual == "TRANSACTION" for expected, actual in predictions)
    transaction_rows = [(expected, actual) for expected, actual in predictions if expected == "TRANSACTION"]
    transaction_recall = sum(expected == actual for expected, actual in transaction_rows) / max(1, len(transaction_rows))
    class_results = {}
    for label in sorted(LABELS):
        rows = [(expected, actual) for expected, actual in predictions if expected == label]
        class_results[label] = (sum(expected == actual for expected, actual in rows), len(rows))
    model_bytes = OUTPUT.stat().st_size
    print(f"model_version={VERSION}")
    print(f"training_samples={len(samples)}")
    print("training_class_counts=" + ",".join(f"{label}:{counts[label]}" for label in sorted(LABELS)))
    print(f"classes={len(labels)}")
    print(f"selected_features={sum(len(value) for value in weights.values())}")
    print(f"model_source_bytes={model_bytes}")
    print(f"evaluation_samples={len(evaluation)}")
    print(f"evaluation_accuracy={accuracy:.3f} ({correct}/{len(evaluation)})")
    print(f"evaluation_transaction_recall={transaction_recall:.3f} ({sum(expected == actual for expected, actual in transaction_rows)}/{len(transaction_rows)})")
    print(f"evaluation_false_positive_transactions={false_positive_transactions}")
    print("evaluation_per_class=" + ",".join(f"{label}:{class_results[label][0]}/{class_results[label][1]}" for label in sorted(LABELS)))
    if model_bytes > MAX_MODEL_SOURCE_BYTES:
        raise SystemExit(f"Generated model is {model_bytes} bytes; limit is {MAX_MODEL_SOURCE_BYTES}")
    if accuracy < MIN_EVALUATION_ACCURACY:
        raise SystemExit(f"Evaluation accuracy {accuracy:.3f} is below required {MIN_EVALUATION_ACCURACY:.2f}")
    if transaction_recall < MIN_TRANSACTION_RECALL:
        raise SystemExit(f"Transaction recall {transaction_recall:.3f} is below required {MIN_TRANSACTION_RECALL:.2f}")
    if false_positive_transactions:
        raise SystemExit(f"Found {false_positive_transactions} non-transaction messages classified as TRANSACTION")

if __name__ == "__main__":
    main()
