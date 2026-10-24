#!/usr/bin/env python3
"""Deterministic PII scrubber and commit guard for SMS corpora.

Standalone and dependency-free. Two jobs:

  scrub_text(s) -> str
      Mask sensitive tokens before any example is written to disk:
        * full card numbers (13-19 digits, optionally grouped)  -> keep last 4
        * full bank account numbers after an account cue         -> keep last 4
        * phone numbers (+91 / 10-digit mobile)                  -> masked
        * OTP / verification codes                               -> masked
        * email addresses                                        -> masked
        * Aadhaar-formatted 12-digit (4 4 4 grouped)             -> masked

  --check <files...>
      Scan data files and fail (exit 1) if any raw PII pattern is present,
      so CI/pre-commit blocks committing un-sanitized SMS.

Deliberately NOT flagged: transaction amounts, already-masked last4
(e.g. XX1234), and contiguous alphanumeric/numeric reference IDs
(UPI/UTR/ARN/txn refs) which are legitimate ledger data, not PII.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

MASK = "[REDACTED]"

# --- detection patterns -----------------------------------------------------
# Card-length digit runs (13-19), optionally space/hyphen grouped in 4s.
_CARD = re.compile(r"\b(?:\d[ -]?){13,19}\b")
# Full account number stated after an explicit, UNMASKED account cue (>=9 digits).
_FULL_ACCOUNT = re.compile(
    r"((?:account|acct|a/c|ac)\s*(?:no\.?|number|#)?\s*)(\d{9,18})\b",
    re.IGNORECASE,
)
# Indian mobile: optional +91/0 prefix then a 10-digit number starting 6-9.
_PHONE = re.compile(r"\b(?:\+?91[-\s]?|0)?([6-9]\d{9})\b")
# OTP / verification / one-time codes.
_OTP = re.compile(
    r"\b(?:otp|one[\s-]?time\s*(?:password|code|pin|passcode)|verification\s*(?:code|otp)|security\s*code)\b"
    r"[^0-9]{0,40}?(\d{4,8})\b",
    re.IGNORECASE,
)
_EMAIL = re.compile(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b")
# Aadhaar: 12 digits grouped as 4 4 4 (space/hyphen) only, to avoid colliding
# with contiguous 12-digit reference IDs.
_AADHAAR = re.compile(r"\b\d{4}[ -]\d{4}[ -]\d{4}\b")


def _last4(digits: str) -> str:
    only = re.sub(r"\D", "", digits)
    return only[-4:] if len(only) >= 4 else only


def _luhn_ok(digits: str) -> bool:
    """True if the digit string passes the Luhn checksum (real card numbers do;
    arbitrary reference/UTR/txn ids almost never do)."""
    only = re.sub(r"\D", "", digits)
    if not 13 <= len(only) <= 19:
        return False
    total, parity = 0, len(only) % 2
    for i, ch in enumerate(only):
        d = int(ch)
        if i % 2 == parity:
            d *= 2
            if d > 9:
                d -= 9
        total += d
    return total % 10 == 0


def _card_sub(m: re.Match) -> str:
    # Only treat Luhn-valid runs as card numbers; leave reference ids untouched.
    return f"XX{_last4(m.group(0))}" if _luhn_ok(m.group(0)) else m.group(0)


def scrub_text(text: str) -> str:
    text = _EMAIL.sub(MASK, text)
    text = _AADHAAR.sub(MASK, text)
    text = _CARD.sub(_card_sub, text)
    text = _FULL_ACCOUNT.sub(lambda m: f"{m.group(1)}XX{m.group(2)[-4:]}", text)
    text = _OTP.sub(lambda m: m.group(0).replace(m.group(1), MASK), text)
    text = _PHONE.sub(MASK, text)
    return text


def scrub_row(row: dict) -> dict:
    row = dict(row)
    row["text"] = scrub_text(row["text"])
    return row


def findings(text: str) -> list[str]:
    """Return human-readable PII hits in `text` (empty list = clean)."""
    hits = []
    for name, pat, grp in (
        ("email", _EMAIL, 0),
        ("aadhaar", _AADHAAR, 0),
        ("card_number", _CARD, 0),
        ("full_account", _FULL_ACCOUNT, 2),
        ("otp_code", _OTP, 1),
        ("phone", _PHONE, 1),
    ):
        for m in pat.finditer(text):
            if name == "card_number" and not _luhn_ok(m.group(0)):
                continue  # reference/UTR/txn id, not a card
            hits.append(f"{name}:{m.group(grp)!r}")
    return hits


def _check(paths: list[str]) -> int:
    failed = 0
    for p in paths:
        path = Path(p)
        for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip():
                continue
            try:
                text = json.loads(line).get("text", "")
            except json.JSONDecodeError:
                text = line
            for hit in findings(text):
                print(f"{path.name}:{lineno}: PII {hit}", file=sys.stderr)
                failed += 1
    if failed:
        print(f"FAIL: {failed} raw PII pattern(s) found", file=sys.stderr)
        return 1
    print(f"OK: no raw PII in {len(paths)} file(s)")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", nargs="+", metavar="FILE",
                        help="scan files and fail on any raw PII pattern")
    args = parser.parse_args()
    if args.check:
        return _check(args.check)
    # demo on stdin-like example
    sample = ("Rs 500 debited from card 4532015112830366. OTP is 445566. "
              "Call +919876543210 or mail john.doe@example.com")
    print("in :", sample)
    print("out:", scrub_text(sample))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
