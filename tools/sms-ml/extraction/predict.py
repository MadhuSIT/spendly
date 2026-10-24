#!/usr/bin/env python3
"""Interactive tester for the trained SMS extraction model.

Loads the exported INT8 ONNX artifact (intent + BIO slot heads) and prints the
predicted message type and extracted fields for any SMS you type. Runs fully
offline against a local model directory.

Usage:
  python predict.py                      # interactive: type a message, press Enter
  python predict.py "Rs 450 debited ..." # one-shot
  python predict.py --model-dir <dir>    # default: a built artifact if found
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import numpy as np
import onnxruntime as ort
from transformers import AutoTokenizer

from train_export import (
    ROOT, MAX_LENGTH, allowed_fields_for_intent, predicted_spans,
)

DEFAULT_DIRS = [
    Path(r"C:\Users\vm30\AppData\Local\Temp\opencode\spendly-build"),
    ROOT / "build/spendly-sms-extractor",
]


def find_model_dir(explicit: str | None) -> Path:
    candidates = [Path(explicit)] if explicit else DEFAULT_DIRS
    for d in candidates:
        if (d / "model-int8.onnx").exists() and (d / "labels.json").exists():
            return d
    raise SystemExit(
        "No exported model found. Train first with train_export.py, or pass "
        "--model-dir. Looked in: " + ", ".join(str(c) for c in candidates)
    )


class Predictor:
    def __init__(self, model_dir: Path):
        self.labels = json.loads((model_dir / "labels.json").read_text())
        self.intents = self.labels["intents"]
        self.tags = self.labels["tags"]
        self.tokenizer = AutoTokenizer.from_pretrained(str(model_dir), use_fast=True)
        self.session = ort.InferenceSession(
            str(model_dir / "model-int8.onnx"),
            providers=["CPUExecutionProvider"],
        )

    def predict(self, text: str) -> dict:
        enc = self.tokenizer(
            text, truncation=True, max_length=MAX_LENGTH, padding="max_length",
            return_offsets_mapping=True, return_attention_mask=True,
        )
        offsets = enc["offset_mapping"]
        feeds = {
            "input_ids": np.array([enc["input_ids"]], dtype=np.int64),
            "attention_mask": np.array([enc["attention_mask"]], dtype=np.int64),
        }
        intent_logits, token_logits = self.session.run(None, feeds)
        intent = self.intents[int(np.argmax(intent_logits[0]))]
        tag_ids = np.argmax(token_logits[0], axis=-1).tolist()
        spans = predicted_spans(tag_ids, offsets, text)
        allowed = allowed_fields_for_intent(intent)
        slots: dict[str, str] = {}
        for field, value in spans:
            if field in allowed:
                slots[field] = value
        return {"intent": intent, "slots": slots}


def _show(text: str, result: dict) -> None:
    print(f"  message : {text}")
    print(f"  type    : {result['intent']}")
    if result["slots"]:
        for field, value in result["slots"].items():
            print(f"  {field:<13}: {value}")
    else:
        print("  (no fields extracted)")
    print()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("text", nargs="*", help="SMS text to classify (one-shot)")
    parser.add_argument("--model-dir", help="directory with model-int8.onnx + labels.json")
    args = parser.parse_args()

    predictor = Predictor(find_model_dir(args.model_dir))

    if args.text:
        text = " ".join(args.text)
        _show(text, predictor.predict(text))
        return 0

    print("Paste an SMS and press Enter (blank line or Ctrl+C to quit).\n")
    try:
        while True:
            text = input("sms> ").strip()
            if not text:
                break
            _show(text, predictor.predict(text))
    except (EOFError, KeyboardInterrupt):
        print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
