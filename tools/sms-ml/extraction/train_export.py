#!/usr/bin/env python3
"""Fine-tune a shared MobileBERT encoder for SMS intent + BIO slot extraction."""
from __future__ import annotations

import argparse
import json
import random
from collections import defaultdict
from pathlib import Path

import torch
from torch import nn
from torch.utils.data import DataLoader, Dataset
from transformers import AutoModel, AutoTokenizer
from onnxruntime.quantization import QuantType, quantize_dynamic

ROOT = Path(__file__).resolve().parents[3]
DATA = ROOT / "tools/sms-ml/extraction"
BASE_MODEL = "google/mobilebert-uncased"
INTENTS = ["TRANSACTION", "AMBIGUOUS", "BALANCE", "FAILED", "MARKETING", "OTHER", "OTP", "REMINDER", "SECURITY_ALERT"]
FIELDS = ["AMOUNT", "DIRECTION", "ACCOUNT_LAST4", "MERCHANT", "DATE", "REFERENCE"]
TAGS = ["O"] + [tag for field in FIELDS for tag in (f"B-{field}", f"I-{field}")]
TAG_TO_ID = {tag: i for i, tag in enumerate(TAGS)}
INTENT_TO_ID = {label: i for i, label in enumerate(INTENTS)}
MAX_LENGTH = 160
SEED = 20261024


def read_rows(path: Path):
    rows = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]
    for i, row in enumerate(rows, 1):
        if row.get("intent") not in INTENT_TO_ID:
            raise ValueError(f"{path.name}:{i}: unknown intent {row.get('intent')!r}")
        text = row.get("text", "")
        if not isinstance(text, str) or not text.strip():
            raise ValueError(f"{path.name}:{i}: text is required")
        occupied = []
        for field, value in row.get("slots", {}).items():
            if field not in FIELDS:
                raise ValueError(f"{path.name}:{i}: unknown slot {field!r}")
            if not isinstance(value, str) or not value or value not in text:
                raise ValueError(f"{path.name}:{i}: slot {field} must be a non-empty exact substring")
            start = text.index(value)
            end = start + len(value)
            if any(start < old_end and end > old_start for old_start, old_end in occupied):
                raise ValueError(f"{path.name}:{i}: overlapping slot spans are not supported")
            occupied.append((start, end))
    return rows


def encode_row(row, tokenizer):
    text = row["text"]
    encoded = tokenizer(text, truncation=True, max_length=MAX_LENGTH, padding="max_length",
                        return_offsets_mapping=True, return_attention_mask=True)
    offsets = encoded.pop("offset_mapping")
    labels = [TAG_TO_ID["O"]] * len(offsets)
    spans = []
    for field, value in row.get("slots", {}).items():
        start = text.index(value)
        end = start + len(value)
        spans.append((start, end, field))
    for start, end, field in spans:
        matching = [i for i, (a, b) in enumerate(offsets) if b > a and a < end and b > start]
        if not matching:
            raise ValueError(f"Could not align {field} span {value!r} in {text!r}")
        for position, token_index in enumerate(matching):
            tag = ("B-" if position == 0 else "I-") + field
            labels[token_index] = TAG_TO_ID[tag]
    # Ignore special tokens and padding for token loss.
    for i, (start, end) in enumerate(offsets):
        if start == end:
            labels[i] = -100
    return {
        "input_ids": torch.tensor(encoded["input_ids"], dtype=torch.long),
        "attention_mask": torch.tensor(encoded["attention_mask"], dtype=torch.long),
        "intent_labels": torch.tensor(INTENT_TO_ID[row["intent"]], dtype=torch.long),
        "token_labels": torch.tensor(labels, dtype=torch.long),
        "offsets": offsets,
        "text": text,
        "intent": row["intent"],
        "slots": row.get("slots", {}),
    }


class SmsDataset(Dataset):
    def __init__(self, rows, tokenizer):
        self.rows = [encode_row(row, tokenizer) for row in rows]

    def __len__(self):
        return len(self.rows)

    def __getitem__(self, index):
        return self.rows[index]


class MultiTaskMobileBert(nn.Module):
    def __init__(self, checkpoint: str, num_intents: int, num_tags: int):
        super().__init__()
        self.encoder = AutoModel.from_pretrained(checkpoint)
        hidden = self.encoder.config.hidden_size
        self.dropout = nn.Dropout(0.1)
        self.intent_head = nn.Linear(hidden, num_intents)
        self.token_head = nn.Linear(hidden, num_tags)

    def forward(self, input_ids, attention_mask, token_type_ids=None):
        kwargs = {"input_ids": input_ids, "attention_mask": attention_mask}
        if token_type_ids is not None:
            kwargs["token_type_ids"] = token_type_ids
        hidden = self.encoder(**kwargs).last_hidden_state
        return self.intent_head(self.dropout(hidden[:, 0])), self.token_head(self.dropout(hidden))


def collate(batch):
    return {
        "input_ids": torch.stack([x["input_ids"] for x in batch]),
        "attention_mask": torch.stack([x["attention_mask"] for x in batch]),
        "intent_labels": torch.stack([x["intent_labels"] for x in batch]),
        "token_labels": torch.stack([x["token_labels"] for x in batch]),
    }


def predicted_spans(tag_ids, offsets, text):
    spans = set()
    current_field, current_start, current_end = None, None, None
    def flush():
        if current_field is not None and current_start is not None and current_end is not None:
            spans.add((current_field, text[current_start:current_end]))
    for tag_id, (start, end) in zip(tag_ids, offsets):
        if start == end:
            continue
        tag = TAGS[tag_id]
        if tag.startswith("B-"):
            flush()
            current_field, current_start, current_end = tag[2:], start, end
        elif tag.startswith("I-") and current_field == tag[2:] and current_end == start:
            current_end = end
        else:
            flush()
            current_field, current_start, current_end = None, None, None
    flush()
    return spans


def evaluate(model, dataset, device):
    model.eval()
    intent_correct = 0
    intent_total = 0
    tp, fp, fn = defaultdict(int), defaultdict(int), defaultdict(int)
    with torch.no_grad():
        for row in dataset.rows:
            ids = row["input_ids"].unsqueeze(0).to(device)
            mask = row["attention_mask"].unsqueeze(0).to(device)
            intent_logits, token_logits = model(ids, mask)
            intent_pred = INTENTS[int(intent_logits.argmax(-1).item())]
            intent_correct += int(intent_pred == row["intent"])
            intent_total += 1
            predicted = predicted_spans(token_logits.argmax(-1)[0].cpu().tolist(), row["offsets"], row["text"])
            actual = {(field, value) for field, value in row["slots"].items()}
            for field in FIELDS:
                p = {span for span in predicted if span[0] == field}
                a = {span for span in actual if span[0] == field}
                tp[field] += len(p & a)
                fp[field] += len(p - a)
                fn[field] += len(a - p)
    field_metrics = {}
    total_tp = total_fp = total_fn = 0
    for field in FIELDS:
        p, f, n = tp[field], fp[field], fn[field]
        total_tp += p
        total_fp += f
        total_fn += n
        precision = p / (p + f) if p + f else 0.0
        recall = p / (p + n) if p + n else 0.0
        field_metrics[field] = {"precision": round(precision, 4), "recall": round(recall, 4),
                                "f1": round(2 * precision * recall / (precision + recall), 4) if precision + recall else 0.0,
                                "true_positive": p, "false_positive": f, "false_negative": n}
    precision = total_tp / (total_tp + total_fp) if total_tp + total_fp else 0.0
    recall = total_tp / (total_tp + total_fn) if total_tp + total_fn else 0.0
    return {
        "intent_accuracy": round(intent_correct / intent_total if intent_total else 0.0, 4),
        "intent_correct": intent_correct,
        "intent_total": intent_total,
        "slot_micro_precision": round(precision, 4),
        "slot_micro_recall": round(recall, 4),
        "slot_micro_f1": round(2 * precision * recall / (precision + recall), 4) if precision + recall else 0.0,
        "fields": field_metrics,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", type=Path, default=ROOT / "build/spendly-sms-extractor")
    parser.add_argument("--epochs", type=int, default=5)
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--max-model-mib", type=float, default=50.0)
    args = parser.parse_args()
    random.seed(SEED)
    torch.manual_seed(SEED)
    torch.set_num_threads(min(4, torch.get_num_threads()))
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    train_rows = read_rows(DATA / "train.jsonl")
    validation_rows = read_rows(DATA / "validation.jsonl")
    test_rows = read_rows(DATA / "test.jsonl")
    for split_name, rows in (("train", train_rows), ("validation", validation_rows), ("test", test_rows)):
        missing = sorted(set(INTENTS) - {row["intent"] for row in rows})
        if missing:
            raise SystemExit(f"{split_name} split is missing intent classes: {missing}")
    normalized = lambda rows: {" ".join(row["text"].lower().split()) for row in rows}
    train_texts, validation_texts, test_texts = map(normalized, (train_rows, validation_rows, test_rows))
    if (train_texts & validation_texts) or (train_texts & test_texts) or (validation_texts & test_texts):
        raise SystemExit("Exact normalized message leakage across train/validation/test splits")
    tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL, use_fast=True)
    if not tokenizer.is_fast:
        raise SystemExit("A fast tokenizer is required to align token offsets")
    train_data = SmsDataset(train_rows, tokenizer)
    validation_data = SmsDataset(validation_rows, tokenizer)
    test_data = SmsDataset(test_rows, tokenizer)
    model = MultiTaskMobileBert(BASE_MODEL, len(INTENTS), len(TAGS)).to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=2e-5, weight_decay=0.01)
    loader = DataLoader(train_data, batch_size=args.batch_size, shuffle=True, collate_fn=collate)
    best_loss = float("inf")
    best_state = None
    for epoch in range(args.epochs):
        model.train()
        total_loss = 0.0
        for batch in loader:
            batch = {key: value.to(device) for key, value in batch.items()}
            optimizer.zero_grad(set_to_none=True)
            intent_logits, token_logits = model(batch["input_ids"], batch["attention_mask"])
            loss_intent = nn.functional.cross_entropy(intent_logits, batch["intent_labels"])
            tag_weights = torch.tensor([0.25 if tag == "O" else 1.0 for tag in TAGS], dtype=torch.float32, device=device)
            loss_slots = nn.functional.cross_entropy(token_logits.reshape(-1, len(TAGS)), batch["token_labels"].reshape(-1), weight=tag_weights, ignore_index=-100)
            loss = loss_intent + loss_slots
            loss.backward()
            nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            optimizer.step()
            total_loss += loss.item()
        model.eval()
        validation_loss = 0.0
        with torch.no_grad():
            for batch in DataLoader(validation_data, batch_size=args.batch_size, collate_fn=collate):
                batch = {key: value.to(device) for key, value in batch.items()}
                intent_logits, token_logits = model(batch["input_ids"], batch["attention_mask"])
                validation_loss += nn.functional.cross_entropy(intent_logits, batch["intent_labels"]).item()
                tag_weights = torch.tensor([0.25 if tag == "O" else 1.0 for tag in TAGS], dtype=torch.float32, device=device)
                validation_loss += nn.functional.cross_entropy(token_logits.reshape(-1, len(TAGS)), batch["token_labels"].reshape(-1), weight=tag_weights, ignore_index=-100).item()
        validation_loss /= max(1, len(DataLoader(validation_data, batch_size=args.batch_size, collate_fn=collate)))
        print(f"epoch={epoch + 1} train_loss={total_loss / max(1, len(loader)):.4f} validation_loss={validation_loss:.4f}")
        if validation_loss < best_loss:
            best_loss = validation_loss
            best_state = {key: value.detach().cpu().clone() for key, value in model.state_dict().items()}
    if best_state is not None:
        model.load_state_dict(best_state)
    validation_metrics = evaluate(model, validation_data, device)
    test_metrics = evaluate(model, test_data, device)
    print("validation_metrics=" + json.dumps(validation_metrics, sort_keys=True))
    print("test_metrics=" + json.dumps(test_metrics, sort_keys=True))
    args.output_dir.mkdir(parents=True, exist_ok=True)
    model.eval().cpu()
    tokenizer.save_pretrained(args.output_dir)
    with (args.output_dir / "labels.json").open("w", encoding="utf-8") as f:
        json.dump({"intents": INTENTS, "tags": TAGS, "fields": FIELDS, "base_model": BASE_MODEL}, f, indent=2)
    input_ids = torch.zeros((1, MAX_LENGTH), dtype=torch.long)
    attention_mask = torch.ones((1, MAX_LENGTH), dtype=torch.long)
    float_path = args.output_dir / "model-fp32.onnx"
    quant_path = args.output_dir / "model-int8.onnx"
    torch.onnx.export(model, (input_ids, attention_mask), str(float_path),
                      input_names=["input_ids", "attention_mask"],
                      output_names=["intent_logits", "token_logits"],
                      dynamic_axes={"input_ids": {0: "batch", 1: "sequence"},
                                    "attention_mask": {0: "batch", 1: "sequence"},
                                    "intent_logits": {0: "batch"},
                                    "token_logits": {0: "batch", 1: "sequence"}},
                      opset_version=17, dynamo=False)
    quantize_dynamic(str(float_path), str(quant_path), weight_type=QuantType.QInt8)
    float_path.unlink()
    artifact_files = [p for p in args.output_dir.iterdir() if p.is_file()]
    artifact_bytes = sum(p.stat().st_size for p in artifact_files)
    report = {
        "base_model": BASE_MODEL,
        "seed": SEED,
        "train_rows": len(train_rows),
        "validation_rows": len(validation_rows),
        "test_rows": len(test_rows),
        "max_length": MAX_LENGTH,
        "validation": validation_metrics,
        "test": test_metrics,
        "artifact_bytes": artifact_bytes,
        "artifact_mib": round(artifact_bytes / (1024 * 1024), 3),
        "files": {p.name: p.stat().st_size for p in artifact_files},
        "warning": "Synthetic bootstrap corpus only; metrics are not production-quality evidence.",
    }
    (args.output_dir / "metrics.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print("artifact_mib=" + str(report["artifact_mib"]))
    if artifact_bytes > args.max_model_mib * 1024 * 1024:
        raise SystemExit(f"Quantized model plus tokenizer is {report['artifact_mib']} MiB; limit is {args.max_model_mib} MiB")
    print("model_size_gate=PASS")


if __name__ == "__main__":
    main()
