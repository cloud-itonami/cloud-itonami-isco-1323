(ns construction.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [construction.store :as store]
            [construction.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Trade"})
    (store/register-site! st {:site-id "S-1" :client-id "client-1"
                              :name "riverside-block-4"
                              :permit-issued-day 100 :permit-expiry-day 400
                              :required-inspections #{"footings" "framing" "electrical"}})
    st))

(defn- advance [day inspections]
  {:op :approve-phase-advance :effect :propose :site-id "S-1"
   :as-of-day day :passed-inspections inspections
   :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})
(def ^:private all-inspections #{"footings" "framing" "electrical"})

(deftest ok-within-window-and-complete-inspections
  (let [st (fresh-store)
        v (governor/check req {} (advance 200 all-inspections) st)]
    (is (:ok? v))))

(deftest ok-at-exact-window-edges
  (testing "the permit window boundary is inclusive"
    (let [st (fresh-store)]
      (is (:ok? (governor/check req {} (advance 100 all-inspections) st)))
      (is (:ok? (governor/check req {} (advance 400 all-inspections) st))))))

(deftest ok-with-extra-inspections-beyond-required
  (testing "a superset of the required set still satisfies completeness"
    (let [st (fresh-store)
          v (governor/check req {} (advance 200 (conj all-inspections "plumbing")) st)]
      (is (:ok? v)))))

(deftest hard-on-before-permit-window
  (testing "permit is interval containment, not field discretion"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (advance 50 all-inspections) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :outside-permit-window (:rule %)) (:violations v))))))

(deftest hard-on-after-permit-window
  (let [st (fresh-store)
        v (governor/check req {} (assoc (advance 500 all-inspections) :confidence 0.99) st)]
    (is (:hard? v))
    (is (some #(= :outside-permit-window (:rule %)) (:violations v)))))

(deftest hard-on-incomplete-inspections
  (testing "inspection completeness has no partial credit"
    (let [st (fresh-store)
          v (governor/check req {} (advance 200 #{"footings" "framing"}) st)]
      (is (:hard? v))
      (is (some #(= :inspections-incomplete (:rule %)) (:violations v))))))

(deftest hard-on-unknown-site
  (let [st (fresh-store)
        v (governor/check req {} (assoc (advance 200 all-inspections) :site-id "S-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-site (:rule %)) (:violations v)))))

(deftest hard-on-foreign-site
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (advance 200 all-inspections) st)]
      (is (:hard? v))
      (is (some #(= :site-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (advance 200 all-inspections) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (advance 200 all-inspections) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-occupancy-certificate
  (let [st (fresh-store)
        v (governor/check req {} {:op :issue-occupancy-certificate :effect :propose
                                  :site-id "S-1" :confidence 0.9 :stake :high} st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (advance 200 all-inspections) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
