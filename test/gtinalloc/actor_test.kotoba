(ns gtinalloc.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [gtinalloc.actor :as actor]
            [gtinalloc.advisor :as advisor]
            [gtinalloc.store :as store]))

(defn- low-confidence-advisor
  "Test-only advisor stub that always proposes with confidence below
  `gtinalloc.governor/confidence-floor`, to exercise the escalation
  path independent of the mock advisor's stake-derived confidence."
  []
  (reify advisor/Advisor
    (-advise [_ _store request]
      {:op :issue-gtin :effect :propose :gtin (:gtin request)
       :confidence 0.3 :stake :low
       :rationale "low-confidence test stub"})))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Goods"
                                 :company-prefix "0360002"})
    st))

(deftest commits-a-well-formed-in-prefix-allocation
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :gtin "036000291452" :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-invalid-check-digit-allocation
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :gtin "036000291459" :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest holds-a-duplicate-allocation
  (let [st (fresh-store)]
    (store/issue! st {:client-id "client-1" :gtin "036000291452"})
    (let [graph (actor/build-graph {:store st})
          request {:client-id "client-1" :gtin "036000291452" :stake :low}
          result (actor/run-request! graph request {} "thread-3")]
      (is (= :hold (:disposition (:state result))))
      (is (= 1 (count (store/records-of st "client-1")))))))

(deftest interrupts-then-issues-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (low-confidence-advisor)})
        request {:client-id "client-1" :gtin "036000291452" :stake :low}
        interrupted (actor/run-request! graph request {} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-4")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
