# cloud-itonami-gtin-issuance

Open Business Blueprint: **Independent GS1 Prefix & GTIN Allocation
Service**.

This repository designs a forkable OSS business for an independent
consultant who helps small brand owners register a GS1 company prefix and
correctly allocate GTINs (GTIN-8/12/13/14, i.e. UPC/EAN/JAN) to their
products -- a real, recurring pain point for small manufacturers who
either allocate barcodes incorrectly (duplicate/invalid codes rejected by
retailers) or never register with GS1 at all.

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
domain work -- it is check-digit computation, GS1 prefix-range
bookkeeping and product-registration paperwork. It is in the same digital/
data-service exemption class as
[`cloud-itonami-6310`](https://github.com/cloud-itonami/cloud-itonami-6310)
(HR SaaS replacement): no robot, still a governor-gated actor.

## Core Contract

```text
brand intake + product line
        |
        v
Allocation Advisor -> GTIN Allocation Governor -> issue, or human review
        |
        v
GTIN record (check-digit verified, non-duplicate) + audit ledger
```

No automated allocation can issue a GTIN the governor would refuse
(invalid check digit, prefix-range exhaustion, duplicate) or suppress the
allocation record without governor approval and audit evidence.

## Required capabilities

- :identity
- :forms
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
