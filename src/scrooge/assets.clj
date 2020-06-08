(ns scrooge.assets
  (:require [scrooge.core :refer [convert-amount]]
            [swissknife.core :refer [map-values]]
            [clojure.data.priority-map :as pm]))


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


(defn- rebalance-keep
  [investment-value diff-asset-dollars]
  (loop [dollar-assets (->> diff-asset-dollars
                            (remove (comp neg? second))
                            (into (pm/priority-map-by >)))
         remaining-value investment-value
         allocation-dollars {}]

    (if (or (empty? dollar-assets)
            (zero? remaining-value))
      allocation-dollars

      (let [highest-value (second (peek dollar-assets))
            top-assets (pm/subseq dollar-assets = highest-value)
            remaining-assets (apply dissoc dollar-assets (keys top-assets))]

        (if (empty? remaining-assets)
          (let [needed-spend (* highest-value (count top-assets))]
            (if (<= needed-spend remaining-value)
              (->> (repeat highest-value)
                   (zipmap (keys top-assets))
                   (merge-with + allocation-dollars))
              (->> (repeat (/ remaining-value (count top-assets)))
                   (zipmap (keys top-assets))
                   (merge-with +  allocation-dollars))))

          (let [next-value (second (peek remaining-assets))
                diff-value (- highest-value next-value)
                needed-spend (* diff-value (count top-assets))]

            (if (<= needed-spend remaining-value)
              (recur (->> (repeat next-value)
                          (zipmap (keys top-assets))
                          (into remaining-assets))
                     (- remaining-value needed-spend)
                     (->> (repeat diff-value)
                          (zipmap (keys top-assets))
                          (merge-with + allocation-dollars)))
              (->> (repeat (/ remaining-value (count top-assets)))
                   (zipmap (keys top-assets))
                   (merge-with +  allocation-dollars)))))))))


(defn rebalance-assets
  "Rebalances assets according to the fractions in the passed allocation map,
  tries to zero the assets passed in `investment`

  sets the allocation of assets not found in the map to 0"
  [assets prices allocation investment sell?]
  (let [fillna-allocation (merge (zipmap (keys assets) (repeat 0))
                                 allocation)
        asset-dollars (convert-assets assets prices "$")
        investment-dollars (convert-assets investment prices "$")
        investment-value (apply + (vals investment-dollars))
        total-value (apply + investment-value (vals asset-dollars))
        adjusted-asset-dollars (map-values #(* % total-value) fillna-allocation)
        diff-asset-dollars (merge-with - adjusted-asset-dollars asset-dollars)
        rebalanced-dollars (if sell?
                             diff-asset-dollars
                             (rebalance-keep investment-value diff-asset-dollars))]

    (merge-with / rebalanced-dollars
                (select-keys prices (keys rebalanced-dollars)))))
