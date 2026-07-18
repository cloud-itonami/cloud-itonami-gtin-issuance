(ns gtinalloc.store
  "SSoT for the GS1 Prefix & GTIN Allocation actor (itonami actor
  pattern, ADR-2607011000 / CLAUDE.md Actors section). Modeled on
  cloud-itonami-isco-1324's supplydist.store.

  Domain:

    client — a registered brand/company {:client-id :name
             :company-prefix}. `:company-prefix` is the GS1 company
             prefix (digits-only string) this brand was already
             assigned by its own GS1 Member Organisation — this actor
             does NOT allocate company prefixes itself (that is GS1's
             own registration process, out of scope); it registers the
             prefix a client already holds and gates GTIN allocation
             within it.
    issued — a committed GTIN allocation {:gtin :client-id} — written
             ONLY via issue!. Duplicate detection is set-membership
             against this map's keys: you cannot issue a GTIN that is
             already issued.
    ledger — append-only audit trail, commit or hold.")

(defprotocol Store
  (client [s client-id])
  (issued-record [s gtin])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (issue! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (issued-record [_ gtin] (get-in @a [:issued gtin]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (vals (:issued @a))))
  (ledger [_] (:ledger @a))
  (register-client! [s c]
    (swap! a assoc-in [:clients (:client-id c)] c) s)
  (issue! [s record]
    (swap! a assoc-in [:issued (:gtin record)] record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :issued {} :ledger []} seed)))))
