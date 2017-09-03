(ns scrooge.assets
  (:require [scrooge.core :refer [convert-amount]]))


(defn accounts->assets
  "Given an accounts balance returns a map of all assets to units"
  [accounts]
  (->> (vals accounts)
       (apply merge-with +)))


(defn total-value
  "Returns the total value of the asset map in the passed unit"
  [assets prices unit]
  (letfn [(reducer [acc [asset amt]]
            (+ acc (convert-amount prices asset unit amt)))]
    (reduce reducer 0 assets)))


(defn rebalance-assets
  "Rebalances assets according to the fractions in the passed allocation map

  Only considers assets in the allocation map"
  [assets prices allocation]
  (let [rel-assets (merge (zipmap (keys allocation) (repeat 0.0))
                          (select-keys assets (keys allocation)))
        dollar-total (total-value rel-assets prices "$")]
    (->> (for [[asset amt] rel-assets
               :let [dollar-share (* (allocation asset) dollar-total)]]
           [asset (convert-amount prices "$" asset dollar-share)])
         (into {}))))
