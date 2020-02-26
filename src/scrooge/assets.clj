(ns scrooge.assets
  (:require [scrooge.core :refer [convert-amount]]
            [swissknife.core :refer [map-values]]))


(defn accounts->assets
  "Given an accounts balance returns a map of all assets to units"
  [accounts]
  (->> (vals accounts)
       (apply merge-with +)))


(defn convert-assets
  "Given a map of assets to units, returns a map of assets to amounts
  in the passed unit"
  [assets prices unit]
  (->> (for [[asset units] assets]
         [asset (convert-amount prices asset unit units)])
       (into {})))


(defn rebalance-assets
  "Rebalances assets according to the fractions in the passed allocation map,
  tries to zero the assets passed in `investment`

  sets the allocation of assets not found in the map to 0"
  [assets prices allocation investment]
  (let [fillna-allocation (merge (zipmap (keys assets) (repeat 0))
                                 allocation)
        asset-dollars (convert-assets assets prices "$")
        investment-dollars (convert-assets investment prices "$")
        total-dollars (+ (apply + (vals asset-dollars))
                         (apply + (vals investment-dollars)))
        adjusted-asset-dollars (map-values #(* % total-dollars) fillna-allocation)
        diff-asset-dollars (merge-with - adjusted-asset-dollars asset-dollars)
        diff-assets (merge-with / diff-asset-dollars
                                (select-keys prices (keys diff-asset-dollars)))
        diff-investment (map-values - investment)
        diff-portfolio (merge-with + diff-assets diff-investment)]
    diff-portfolio))
