(ns construction.governor
  "ConstructionManagersGovernor — the independent safety/traceability
  layer for the ISCO-08 1323 community construction managers actor
  (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors section).
  Modeled on cloud-itonami-isco-4311's bookkeeping.governor.
  Construction twist: a phase advance is only admissible when the
  as-of day falls inside the site's registered permit window (interval
  containment) AND every registered required inspection has been
  passed (subset containment) — a site is either permitted and
  inspected, or the advance holds; there is no partial credit.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose.
    3. site basis         — a phase advance must cite a REGISTERED
                           site belonging to this client.
    4. permit window       — the proposed as-of day must satisfy
                           permit-issued-day <= as-of-day <=
                           permit-expiry-day (interval containment
                           against the registered permit).
    5. inspection completeness — the proposed passed-inspections set
                           must be a SUPERSET of the site's registered
                           required-inspections set (no partial
                           credit).
  ESCALATION invariants (:escalate? true, human sign-off):
    6. :op :issue-occupancy-certificate (final sign-off for
                           occupancy).
    7. low confidence (< `confidence-floor`)."
  (:require [clojure.set :as set]
            [construction.store :as store]))

(def confidence-floor 0.6)

(defn- hard-violations [{:keys [request proposal]} client-record s]
  (let [{:keys [op as-of-day passed-inspections]} proposal
        advance? (= :approve-phase-advance op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and advance? (nil? s))
      (conj {:rule :unknown-site :detail "未登録 site への工程承認は不可"})

      (and advance? s (not= (:client-id s) (:client-id request)))
      (conj {:rule :site-wrong-client :detail "site が別 client のもの"})

      (and advance? s (integer? as-of-day)
           (or (< as-of-day (:permit-issued-day s)) (> as-of-day (:permit-expiry-day s))))
      (conj {:rule :outside-permit-window
             :detail (str "day " as-of-day " が許可窓 [" (:permit-issued-day s) ", "
                          (:permit-expiry-day s) "] の外（許可は区間包含であって現場裁量ではない）")})

      (and advance? s
           (not (set/superset? (set passed-inspections) (:required-inspections s))))
      (conj {:rule :inspections-incomplete
             :detail (str "未完了検査 "
                          (vec (set/difference (:required-inspections s) (set passed-inspections)))
                          "（検査完了は集合包含であって部分点はない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `construction.store/Store`. Pure — never
  mutates the store."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        s (some->> (:site-id proposal) (store/site store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record s)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (= :issue-occupancy-certificate (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
