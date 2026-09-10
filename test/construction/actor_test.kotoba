(ns construction.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [construction.actor :as actor]
            [construction.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Trade"})
    (store/register-site! st {:site-id "S-1" :client-id "client-1"
                              :name "riverside-block-4"
                              :permit-issued-day 100 :permit-expiry-day 400
                              :required-inspections #{"footings" "framing"}})
    st))

(deftest commits-an-in-window-fully-inspected-advance
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-phase-advance :stake :low
                 :site-id "S-1" :as-of-day 200
                 :passed-inspections #{"footings" "framing"}}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-incomplete-inspection-advance
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-phase-advance :stake :low
                 :site-id "S-1" :as-of-day 200
                 :passed-inspections #{"footings"}}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-issues-occupancy-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :issue-occupancy-certificate :stake :high
                 :site-id "S-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
