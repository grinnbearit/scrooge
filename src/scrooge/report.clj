(ns scrooge.report
  (:require [clj-time.core :as t]
            [scrooge.core :as sc]
            [scrooge.parse :as sp]
            [scrooge.assets :as sa]
            [clojure.set :as set]))


(def EUR "€")
(def INR "₹")

(def ALLOCATION
  {"NIFTYBEES" 0.16 "GOLDBEES" 0.04
   "BONDMEES" 0.16 "EQTYMEES" 0.64
   EUR 0.0 INR 0.0})

(defn- _net-worth
  [accounts]
  (let [agg (sc/aggregate accounts 0)]
    (merge-with + (agg ["Assets"]) (agg ["Liabilities"]))))


(defn net-worth
  "Returns number of units of each commodity in Assets - Liabilities

  If passed a price-map and a commodity, returns total networth in
  that commodity"
  ([ledger]
   (_net-worth (sc/balance ledger)))
  ([ledger prices commodity]
   (-> (sc/balance ledger)
       (sc/convert-accounts prices commodity)
       (_net-worth))))


(defn portfolio
  "Returns fractional allocation of net-worth in each commodity
  use include/exclude to return relative allocations instead

  tolerance is passed downstream to sc/fractional"
  [ledger prices & {:keys [include exclude tolerance]
                    :or {exclude #{} tolerance 0.001}}]
  (let [commodities (net-worth ledger)
        include (or include (set (keys commodities)))
        included-keys (set/difference include exclude)]
    (-> {:net-worth (select-keys commodities included-keys)}
        (sc/fractional prices :tolerance tolerance)
        (:net-worth))))


(defn valuation
  "Returns current value of each commodity in net-worth"
  [ledger prices commodity]
  (letfn [(reducer [acc [comm units]]
            (assoc acc comm {comm units}))]
    (-> (reduce reducer {} (net-worth ledger))
        (sc/convert-accounts prices commodity))))


(defn asset-delta
  "Returns the number of units of each asset that need to be bought
  to rebalance the portfolio"
  [assets prices allocation]
  (let [rebalanced (sa/rebalance-assets assets prices allocation)]
    (merge-with - rebalanced assets)))
