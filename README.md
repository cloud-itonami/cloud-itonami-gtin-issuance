# cloud-itonami-gtin-issuance

Open Business Blueprint (implemented actor): **Independent GS1 Prefix &
GTIN Allocation Service**.

This repository publishes a forkable OSS business for an independent
consultant who helps small brand owners correctly allocate GTINs
(GTIN-8/12/13/14, i.e. UPC/EAN/JAN) within a GS1 company prefix they
already hold -- a real, recurring pain point for small manufacturers who
either allocate barcodes incorrectly (duplicate/invalid codes rejected by
retailers) or never register with GS1 at all. **This repo does not
register a company prefix with GS1 itself** (that is GS1's own Member
Organisation process, out of scope) -- it registers the prefix a client
already holds and gates GTIN allocation within it.

## Why this is not code-keyed like ISIC/ISCO/COFOG/UNSPSC blueprints

GTIN is an **identifier system**, not a classification taxonomy -- a GTIN
identifies one product, it does not have a category hierarchy a business
niche could be keyed to (see ADR-2607031800). `cloud-itonami-gtin-*`
splits by FUNCTION instead: this repo (issuance), plus
[`cloud-itonami-gtin-verification`](https://github.com/cloud-itonami/cloud-itonami-gtin-verification)
and
[`cloud-itonami-gtin-catalog`](https://github.com/cloud-itonami/cloud-itonami-gtin-catalog).

## No robotics premise

Unlike most `cloud-itonami-*` verticals, this business has no physical
domain work -- it is check-digit computation and registry bookkeeping. It
is in the same digital/data-service exemption class as
[`cloud-itonami-6310`](https://github.com/cloud-itonami/cloud-itonami-6310)
(HR SaaS replacement): no robot, still a governor-gated actor.

**Maturity: `:implemented`** -- Allocation Advisor ⊣ GTIN Allocation
Governor as a langgraph-clj StateGraph (`intake -> advise -> govern ->
decide -> commit/hold`, human-approval interrupt), modeled on
cloud-itonami-isco-1324's supply-distribution actor. 17 tests / 32
assertions green (`kbb -M:test`).

The one externally-verifiable rule this governor enforces is the REAL
**GS1 GTIN Modulo-10 check-digit algorithm** (GS1 General Specifications
section 7.9 -- the same algorithm underlying every UPC-A / EAN-13(JAN) /
EAN-8 / ITF-14 barcode in circulation). `gtinalloc.governor/check-digit`
is verified in `test/gtinalloc/governor_test.kotoba` against two
independently-known reference GTINs: UPC-A `036000291452` (Wrigley's gum,
check digit `2`) and EAN-13 `4006381333931` (GS1's own commonly cited
worked example, check digit `1`).

## Core Contract

```text
brand intake (registered company-prefix) + product line
        |
        v
Allocation Advisor -> GTIN Allocation Governor -> issue, or human review
        |
        v
GTIN record (check-digit verified, in-prefix, non-duplicate) + audit ledger
```

The HARD invariants -- arithmetic against the GS1 standard and set
membership against this actor's own registry, not a fabricated external
rule:

1. **Check-digit correctness** -- the GTIN's own last digit must equal
   the GS1 Modulo-10 check digit computed over the preceding digits (the
   real, cited GS1 standard above).
2. **Format** -- the GTIN must be an all-digit string of length 8, 12,
   13 or 14 (the four GS1 GTIN lengths).
3. **Prefix membership** -- the GTIN's payload must start with the
   client's registered company-prefix (this actor's own registry
   invariant -- see `gtinalloc.governor` ns docstring for the honest
   GTIN-8 simplification this makes).
4. **Duplicate-gtin** -- the exact GTIN must not already be issued (you
   cannot issue what is already issued).

Also HARD: unregistered client, non-`:propose` effect. Escalation
(human sign-off): low confidence (< 0.6).

**Not yet implemented / honestly out of scope:** GS1 company-prefix
registration itself (a client must already hold one), and GS1's own
company-prefix "range exhaustion" bookkeeping (this actor tracks
per-GTIN duplication, not a shrinking allocatable-range counter).

## Run

```bash
kbb -M:test    # governor contract (incl. the two real-GTIN check-digit
                    # fixtures) + actor lifecycle, 17 tests / 32 assertions
```

## Layout

| File | Role |
|---|---|
| `src/gtinalloc/store.kotoba` | **Store** protocol -- `MemStore`; registered clients (company-prefix), issued GTINs, append-only audit ledger. |
| `src/gtinalloc/advisor.kotoba` | **Allocation Advisor** -- `mock-advisor` \\| `llm-advisor`; proposes a GTIN allocation. |
| `src/gtinalloc/governor.kotoba` | **GTIN Allocation Governor** -- the real GS1 check-digit algorithm + format/prefix/duplicate HARD checks + confidence gate. |
| `src/gtinalloc/actor.kotoba` | **GTINAllocationActor** -- langgraph-clj StateGraph. |
| `test/gtinalloc/*_test.clj` | governor contract (incl. real-GTIN check-digit fixtures) + actor lifecycle. |

## Required capabilities

- :identity
- :forms
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
