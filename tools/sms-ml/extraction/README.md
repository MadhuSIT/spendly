# Spendly multitask SMS extraction prototype

## Goal
Train one on-device language model with two outputs: message intent classification using Spendly's existing labels, and token-level extraction for amount, direction cue, masked account last four, merchant, date, and reference.

This is a benchmark/prototype stage. It does **not** replace SmsParsers.kt or feed the ledger until it passes field-level evaluation and is integrated into Android with deterministic validation. The model must never choose a linked financial entity, deduplicate ledger events, or write to Room.

## Candidate
- Base checkpoint: google/mobilebert-uncased
- Fine-tuning: shared MobileBERT encoder with an intent classification head and token-classification head.
- Export: ONNX, then dynamic INT8 quantization.
- Tokenizer files are included in the artifact.
- CI checks the exported model artifact is below 50 MiB. The measured artifact size, field-level scores, intent metrics, and latency benchmark must be reviewed before runtime integration.

## Data
JSONL files contain text, intent, and slots. Each slot value must be an exact substring of its text. All messages are synthetic/sanitized; never commit raw inbox SMS, OTPs, phone numbers, or account/reference identifiers. The current corpus is only a bootstrap set and is not enough to establish production quality. Add reviewed and diverse examples before relying on it.

Splits are fixed: train.jsonl, validation.jsonl, and test.jsonl. The training script imports unique examples from Spendly's existing synthetic intent corpora as weakly labelled training rows, using conservative regexes for amount, account suffix, reference, date, and direction cues. These labels may be noisy and do not provide merchant supervision. It also generates deterministic, fully annotated transaction examples from varied templates and a merchant vocabulary, adding them to training only. Validation/test rows are never added to training, and exact normalized text duplicates are excluded. The evaluation applies intent-aware field constraints (for example, no merchant/reference extraction for OTP or marketing messages) to reduce implausible false-positive fields. Do not move test examples into training or tune thresholds on the test split.

## Local training
Python 3.11+ with network access to the Hugging Face model hub is required for the first checkpoint download:

    python -m pip install -r tools/sms-ml/extraction/requirements.txt
    python tools/sms-ml/extraction/train_export.py --output-dir build/spendly-sms-extractor

The script trains a shared encoder, reports exact-span field precision/recall/F1 and intent accuracy, exports ONNX, dynamically quantizes weights, and fails if model plus tokenizer files exceed 50 MiB. Model-hub downloads happen only during developer/CI training; Android inference is intended to be offline.

## Safety gates before Android integration
1. Review held-out test results, including each field separately and false-positive slot rates. The expanded synthetic validation/test splits are still not evidence of production accuracy.
2. Add a larger independently reviewed corpus representing multiple banks, card issuers, UPI apps, wallets, English variants, and malformed/ambiguous messages.
3. Compare against the current Kotlin parser on exactly the same held-out SMS set.
4. Integrate ONNX Runtime Android only after the model artifact, latency, memory, and APK-size costs are measured.
5. Keep deterministic amount validation, entity resolution, deduplication, review state, and Room persistence authoritative.

The prototype must not be described as production-ready based only on synthetic examples.