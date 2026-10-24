# Spendly on-device SMS ML baseline

## Included files

- `training_corpus.tsv`: 54 general synthetic examples across the existing message classes.
- `linked_account_scenarios.tsv`: 38 synthetic examples with consistent bank-account/card identifiers, linked purchases and refunds, a matched bank-to-card payment, statements, balances, an IPO block/release, offers, and wallet activity.
- `field_regressions.tsv`: 18 sanitized, real-world-pattern examples based on messages reviewed during phone/inbox discussion. Account suffixes, recipient names, UTRs, references, and other identifiers are removed or replaced with `[REDACTED]`.
- `evaluation_corpus.tsv`: the original held-out synthetic examples.
- `field_evaluation.tsv`: 9 held-out paraphrases for dividend credits, NEFT/RTGS, card payments, failed bill payments, wallet activity, railway booking, and charges. These are not used to fit weights.

The trainer combines the three training files (110 examples total); the two held-out evaluation files contain 28 examples total.
- `train_sms_model.py`: deterministic Python-standard-library trainer; no network or real inbox access.
- `app/src/main/java/com/madhusit/spendly/domain/sms/SmsMlModelData.kt`: generated compact one-vs-rest log-odds weights used by Android.
- `OnDeviceSmsClassifier`: local inference plus conservative safety overrides and confidence gates.

This is an initial statistical baseline, not a claim that 54 synthetic examples cover the diversity of bank SMS. It must be evaluated and expanded using sanitized, reviewed examples before being treated as production-grade.

## Rebuild the model

From the repository root:

```bash
python3 tools/sms-ml/train_sms_model.py
```

The script regenerates `SmsMlModelData.kt` deterministically and prints sample counts, per-class counts, selected feature count, generated source byte size, overall held-out accuracy, per-class results, transaction recall, and non-transaction false positives. It fails if held-out accuracy falls below 80%, transaction recall falls below 70%, any held-out non-transaction is classified as a transaction, or the generated Kotlin source exceeds 25 KiB. The model retains 40 features per class (up from 14) to better represent real SMS phrasing.

Run the independent ML checks with:

```bash
python3 tools/sms-ml/validate_sms_ml.py
python3 tools/sms-ml/train_sms_model.py
python3 tools/sms-ml/validate_sms_ml.py
git diff --exit-code -- app/src/main/java/com/madhusit/spendly/domain/sms/SmsMlModelData.kt
```

`.github/workflows/sms-ml.yml` runs this fast pipeline independently from Android Gradle/unit/instrumentation tests. It triggers for ML corpus/trainer/model changes and supports manual dispatch.

## Runtime behavior

```
SMS receiver / inbox reconciliation
  -> SmsParserEngine
  -> SmsDetector
  -> OnDeviceSmsClassifier (offline)
      -> safety overrides for security alerts, OTP, reminders, balance-only,
         failed and marketing messages
      -> compact statistical model
      -> confidence gate: uncertain results become AMBIGUOUS
  -> existing provider/generic extraction
  -> existing normalized-result validation
  -> entity resolution and deduplication
  -> existing Room ledger / review state
```

The model never writes to Room and never chooses a financial entity. It only gates messages into the existing parser. The ledger and repository validation remain authoritative. Ambiguous classification returns `AMBIGUOUS_TRANSACTION` before extraction, so it cannot become an auto-confirmed expense.

## Feedback and safe retraining

1. During phone testing, note the prediction and class for a message. Do not copy raw personal SMS into GitHub issues, logs, or the repository.
2. If the prediction is wrong, create a sanitized synthetic equivalent that preserves the relevant linguistic pattern but removes account numbers, phone numbers, names, references, OTPs and real amounts.
3. Have the correct class explicitly reviewed before adding the sanitized example to `training_corpus.tsv`. Never use the model's unreviewed prediction as its own label.
4. Add a different held-out sanitized example to `evaluation_corpus.tsv` so the new behavior is measured rather than memorized.
5. Run the trainer, inspect metrics and generated diff, run unit/instrumentation tests, build and install the app, then compare the new model against the previous version.
6. Change the model version deliberately and keep the previous version available in Git for rollback. Do not auto-retrain from raw phone SMS and do not upload SMS to a service.

## Known limitations

- The corpus remains small and the field examples are sanitized paraphrases, not raw inbox exports. Real-world provider and language variation is not represented adequately.
- The evaluation set is small; its accuracy is directional, not a production quality guarantee.
- This first slice adds ML to message classification only; it does not claim to solve semantic deduplication of different SMS wording.
- A mixed security/transaction notification may be held out as a security alert when no explicit completed money-movement verb is present. Review ambiguous messages rather than guessing.
- Device-level SMS permission, delivery, lifecycle and complete ledger behavior still require physical-device validation.
