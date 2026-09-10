(ns gtinalloc.advisor
  "Allocation Advisor — proposes a GTIN allocation for a registered
  client's product. Swappable mock/llm; the advisor ONLY proposes —
  `gtinalloc.governor` checks GS1 check-digit correctness, prefix
  membership, and duplication independently. Modeled on
  cloud-itonami-isco-1324's supplydist.advisor.

  A proposal: {:op :issue-gtin :effect :propose :gtin str
               :confidence n :stake kw :rationale str}"
  ;; clojure.edn, not clojure.core/read-string: this parses untrusted
  ;; advisor output, and the core reader executes #=(...) at read time.
  (:require [clojure.edn :as edn]))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [gtin stake] :as request}]
  {:op :issue-gtin
   :effect :propose
   :gtin gtin
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed GTIN allocation for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a GS1 GTIN allocation advisor. Given a request, propose the
   :gtin to allocate, an honest :confidence and a :stake. Never
   propose a GTIN with an invalid GS1 check digit or one outside the
   client's registered company prefix — the governor independently
   recomputes both against the registered client record.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "allocation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
