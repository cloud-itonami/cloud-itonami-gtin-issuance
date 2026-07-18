(ns gtinalloc.governor
  "GTINAllocationGovernor — the independent invariant layer for the
  GS1 Prefix & GTIN Allocation actor (itonami actor pattern,
  ADR-2607011000 / CLAUDE.md Actors section). Modeled on
  cloud-itonami-isco-1324's supplydist.governor.

  The one externally-verifiable rule this governor enforces is REAL:
  the GS1 GTIN Modulo-10 check-digit algorithm (GS1 General
  Specifications section 7.9, 'GTIN Check Digit Calculation' — the
  same algorithm underlying every UPC-A / EAN-13(JAN) / EAN-8 / ITF-14
  barcode in circulation). `check-digit` below is verified against two
  independently-known reference GTINs: UPC-A 036000291452 (Wrigley's
  gum, check digit 2) and EAN-13 4006381333931 (GS1's own commonly
  cited worked example, check digit 1) — both round-trip correctly
  (see `governor_test.clj`).

  Honest simplification: GTIN-8 codes are, in the real world, issued
  directly by a GS1 Member Organisation from a separate restricted
  'GS1-8 Prefix' pool rather than sliced from a company's general GCP
  — this governor still requires a GTIN-8 payload to start with the
  client's registered `:company-prefix` string, which is this actor's
  own registry-consistency invariant, not a claim about how GS1 itself
  allocates GTIN-8s. A real deployment allocating GTIN-8s should record
  whatever prefix string its GS1 MO actually assigned there.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance      — the client must be registered (with a
                                company-prefix).
    2. no-actuation            — proposal :effect must be :propose.
    3. gtin format              — the GTIN must be an all-digit string
                                of length 8, 12, 13 or 14 (the four GS1
                                GTIN lengths).
    4. check-digit correctness  — the GTIN's own last digit must equal
                                the GS1 Modulo-10 check digit computed
                                over the preceding digits (REAL, cited
                                standard — see ns docstring).
    5. prefix membership        — the GTIN's payload must start with
                                the client's registered company-prefix
                                (arithmetic string check, the same
                                category as cloud-itonami-isco-1324's
                                carrier-membership check — an internal
                                registry invariant, not a second
                                external legal citation).
    6. duplicate-gtin            — the exact GTIN must not already be
                                issued (set membership — 'you cannot
                                issue what is already issued', mirrors
                                cloud-itonami-isco-1324's stock-
                                arithmetic check).
  ESCALATION invariants (:escalate? true, human sign-off):
    7. low confidence (< `confidence-floor`)."
  (:require [clojure.string :as str]
            [gtinalloc.store :as store]))

(def confidence-floor 0.6)
(def ^:private valid-lengths #{8 12 13 14})

(defn- digit-string? [s]
  (boolean (and (string? s) (seq s) (re-matches #"\d+" s))))

(defn- digits [s]
  (mapv #(- (int %) (int \0)) s))

(defn check-digit
  "GS1 Modulo-10 check digit (GS1 General Specifications section 7.9).
  `payload-digits` is the GTIN without its own check digit — a
  collection of ints, most-significant digit first. Multiply the
  digit immediately left of the (absent) check digit by 3, alternate
  1/3 moving left, sum, and the check digit is whatever brings the
  sum to the next multiple of 10."
  [payload-digits]
  (let [weighted (->> payload-digits
                       reverse
                       (map-indexed (fn [i d] (* d (if (even? i) 3 1)))))
        total (reduce + weighted)]
    (mod (- 10 (mod total 10)) 10)))

(defn valid-gtin?
  "True if `gtin` is a digit-only string of a valid GTIN length AND
  its own last digit matches the GS1 check digit computed over the
  rest."
  [gtin]
  (and (digit-string? gtin)
       (contains? valid-lengths (count gtin))
       (let [ds (digits gtin)]
         (= (last ds) (check-digit (butlast ds))))))

(defn- prefix-match? [gtin company-prefix]
  (let [payload (subs gtin 0 (dec (count gtin)))                 ; drop check digit
        window (if (= 14 (count gtin)) (subs payload 1) payload)] ; drop GTIN-14 indicator digit
    (str/starts-with? window (or company-prefix ""))))

(defn- hard-violations [{:keys [proposal]} client-record gtin-already-issued?]
  (let [{:keys [gtin]} proposal
        well-formed? (and (digit-string? gtin) (contains? valid-lengths (count gtin)))]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client（company-prefix 未登録）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (not well-formed?)
      (conj {:rule :invalid-length
             :detail (str "GTIN は数字のみ・8/12/13/14桁である必要がある (" (pr-str gtin) ")")})

      (and well-formed? (not (valid-gtin? gtin)))
      (conj {:rule :invalid-check-digit
             :detail "GS1 Modulo-10 check digit 不一致（GS1 General Specifications §7.9）"})

      (and client-record well-formed? (valid-gtin? gtin)
           (not (prefix-match? gtin (:company-prefix client-record))))
      (conj {:rule :prefix-mismatch
             :detail (str "GTIN が client 登録済み company-prefix "
                          (:company-prefix client-record) " の外")})

      gtin-already-issued?
      (conj {:rule :duplicate-gtin
             :detail (str "GTIN " gtin " は既に発行済み（存在するものは再発行できない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `gtinalloc.store/Store`. Pure — never mutates
  the store."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        gtin (:gtin proposal)
        already-issued? (boolean (and gtin (store/issued-record store gtin)))
        hard (hard-violations {:proposal proposal} client-record already-issued?)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)]
    {:ok? (and (not hard?) (not low?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) low?)}))
