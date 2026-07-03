# Business Model: Independent GS1 Prefix & GTIN Allocation Service

## Classification

- Repository: `cloud-itonami-gtin-issuance`
- Domain: product-identity / GTIN issuance (function-keyed, not
  code-keyed -- see README)
- Social impact: small manufacturers gain retail-ready barcodes without
  hiring a compliance consultant

## Customer

- small manufacturers and CPG (consumer packaged goods) startups launching
  a first retail product
- Etsy/Amazon-scale sellers who need retailer-recognized GTINs, not
  marketplace-only ASINs
- co-packers and private-label brands managing many small brand clients

## Offer

- GS1 Member Organization prefix registration guidance
- correct GTIN-8/12/13/14 allocation per product/variant/pack-size
  (following GS1 allocation rules, e.g. new GTIN required on a
  materially different product)
- check-digit-verified issuance record
- duplicate/conflict detection against the operator's own allocation
  history
- allocation-history audit export for retailer compliance audits

## Revenue

- per-prefix registration consulting fee
- per-GTIN allocation fee (or bundled allocation packages)
- ongoing compliance subscription for brands with frequent SKU launches

## Trust Controls

- every allocation requires GTIN Allocation Governor clearance
  (check-digit valid, no duplicate, prefix range not exhausted)
- allocation to an already-issued GTIN is a hard hold, never overridable
- every allocation and hold is logged
- public allocation-count claims must reference the audit ledger
