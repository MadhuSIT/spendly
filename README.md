# Spendly

Local-first Android personal finance ledger.

## Product

Spendly automatically converts financial transaction SMS into a normalized, auditable ledger. The ledger—not raw SMS and not derived reports—is the source of truth.

## Documentation

- [Product Specification](docs/PRODUCT_SPEC.md)
- [Architecture](docs/ARCHITECTURE.md)

## Core flow

`SMS_RECEIVED -> classify -> identify provider -> parse -> normalize -> validate -> deduplicate -> persist ledger -> categorize -> notify`

Reconciliation uses the same pipeline with an overlap window so real-time ingestion and recovery converge idempotently.

## Product principles

- Local-first and privacy-preserving
- No permanent foreground service
- Financial correctness over convenience
- Idempotent ingestion and deterministic deduplication
- User corrections are authoritative
- Derived reports must be reproducible from the ledger
