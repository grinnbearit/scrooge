(ns scrooge.report
  (:require [clj-time.core :as t]
            [scrooge.core :as sc]
            [scrooge.parse :as sp]))


(def EUR "€")
(def INR "₹")


(defn- _net-worth
  [accounts]
  (let [agg (sc/aggregate accounts 0)]
    (merge-with + (agg ["Assets"]) (agg ["Liabilities"]))))


(defn net-worth
  ([ledger]
   (_net-worth (sc/balance ledger)))
  ([ledger prices commodity]
   (-> (sc/balance ledger)
       (sc/convert-accounts prices commodity)
       (_net-worth))))
