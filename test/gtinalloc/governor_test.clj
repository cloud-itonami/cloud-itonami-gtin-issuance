(ns gtinalloc.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [gtinalloc.store :as store]
            [gtinalloc.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Goods"
                                 :company-prefix "0360002"})
    st))

(def ^:private req {:client-id "client-1"})

(defn- propose [gtin]
  {:op :issue-gtin :effect :propose :gtin gtin :confidence 0.9 :stake :low})

;; --- check-digit correctness, verified against two independently-known
;; --- real-world GTINs (not fabricated test fixtures).

(deftest check-digit-matches-real-upc-a
  (testing "Wrigley's gum UPC-A 036000291452, check digit is 2"
    (is (= 2 (governor/check-digit [0 3 6 0 0 0 2 9 1 4 5])))))

(deftest check-digit-matches-real-ean-13
  (testing "GS1's own worked example 4006381333931, check digit is 1"
    (is (= 1 (governor/check-digit [4 0 0 6 3 8 1 3 3 3 9 3])))))

(deftest valid-gtin-accepts-real-upc-a
  (is (governor/valid-gtin? "036000291452")))

(deftest valid-gtin-rejects-bad-check-digit
  (is (not (governor/valid-gtin? "036000291459"))))

;; --- governor contract

(deftest ok-well-formed-in-prefix-not-duplicate
  (let [st (fresh-store)
        v (governor/check req {} (propose "036000291452") st)]
    (is (:ok? v))))

(deftest hard-on-invalid-check-digit
  (let [st (fresh-store)
        v (governor/check req {} (propose "036000291459") st)]
    (is (:hard? v))
    (is (some #(= :invalid-check-digit (:rule %)) (:violations v)))))

(deftest hard-on-invalid-length
  (let [st (fresh-store)
        v (governor/check req {} (propose "12345") st)]
    (is (:hard? v))
    (is (some #(= :invalid-length (:rule %)) (:violations v)))))

(deftest hard-on-non-digit-gtin
  (let [st (fresh-store)
        v (governor/check req {} (propose "03600029145X") st)]
    (is (:hard? v))
    (is (some #(= :invalid-length (:rule %)) (:violations v)))))

(deftest hard-on-prefix-mismatch
  (let [st (fresh-store)
        ;; a well-formed, check-digit-VALID GTIN-12 (verified via
        ;; governor/check-digit on payload 99900012345 -> 4) whose
        ;; prefix "9990001" differs from the registered client prefix
        v (governor/check req {} (propose "999000123454") st)]
    (is (:hard? v))
    (is (some #(= :prefix-mismatch (:rule %)) (:violations v)))))

(deftest hard-on-duplicate-gtin
  (let [st (fresh-store)]
    (store/issue! st {:client-id "client-1" :gtin "036000291452"})
    (let [v (governor/check req {} (propose "036000291452") st)]
      (is (:hard? v))
      (is (some #(= :duplicate-gtin (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (propose "036000291452") st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (propose "036000291452") :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (propose "036000291452") :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
