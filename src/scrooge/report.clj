(ns scrooge.report
  (:require [clj-time.core :as t]
            [scrooge.core :as sc]
            [scrooge.parse :as sp]
            [clojure.set :as set]))


(def EUR "€")
(def INR "₹")


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
