(ns construction.advisor
  "ConstructionManagersAdvisor — proposes a construction-site
  operation (approve a phase advance, issue an occupancy certificate)
  for a registered organization. Swappable mock/llm; the advisor ONLY
  proposes — `construction.governor` checks the permit window and
  inspection completeness independently. Modeled on
  cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-phase-advance|:issue-occupancy-certificate
               :effect :propose :site-id str :as-of-day int
               :passed-inspections #{str} :stake kw :confidence n
               :rationale str}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake site-id as-of-day passed-inspections] :as request}]
  {:op op
   :effect :propose
   :site-id site-id
   :as-of-day as-of-day
   :passed-inspections passed-inspections
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a construction management advisor. Given a request, propose
   an :op, the :site-id, :as-of-day and :passed-inspections, an honest
   :confidence and a :stake. Never call a phase advance conforming
   outside the permit window or with incomplete inspections — the
   governor checks both against the registered site record.")

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
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
