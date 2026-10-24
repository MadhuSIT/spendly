#!/usr/bin/env python3
"""Report the final held-out synthetic test set; it is never used to fit weights."""
from collections import Counter
from train_sms_model import CORPORA, TEST_DATASET, read_corpus, train, predict, LABELS

def main():
    training = [row for path in CORPORA for row in read_corpus(path)]
    labels, weights, biases = train(training)
    rows = read_corpus(TEST_DATASET)
    results = [(expected, predict(text, labels, weights, biases)) for expected, text in rows]
    correct = sum(expected == actual for expected, actual in results)
    false_positive = sum(expected != "TRANSACTION" and actual == "TRANSACTION" for expected, actual in results)
    per_class = {}
    for label in sorted(LABELS):
        selected = [(expected, actual) for expected, actual in results if expected == label]
        per_class[label] = (sum(expected == actual for expected, actual in selected), len(selected))
    accuracy = correct / max(1, len(results))
    transaction = per_class["TRANSACTION"]
    recall = transaction[0] / max(1, transaction[1])
    print(f"final_test_samples={len(rows)}")
    print(f"final_test_accuracy={accuracy:.3f} ({correct}/{len(rows)})")
    print(f"final_test_transaction_recall={recall:.3f} ({transaction[0]}/{transaction[1]})")
    print(f"final_test_false_positive_transactions={false_positive}")
    print("final_test_per_class=" + ",".join(f"{label}:{per_class[label][0]}/{per_class[label][1]}" for label in sorted(LABELS)))
    if accuracy < 0.80:
        raise SystemExit("Final test accuracy below 80%")
    if false_positive:
        raise SystemExit("Final test has non-transaction false positives")
if __name__ == "__main__":
    main()
