(ns scrooge.assets
  (:require [scrooge.core :refer [convert-amount]]))


(defn accounts->assets
  "Given an accounts balance returns a map of all assets to prices"
  [accounts]
  (->> (for [[comm value] (->> (vals accounts)
                               (apply merge-with +))]
         [comm {comm value}])
       (into {})))


(defn convert-assets
  "Converts assets into a specific unit, doesn't remove
  the existing units"
  [assets prices to]
  (->> (for [[asset units] assets
             :let [to-units (convert-amount prices asset to (units asset))]]
         [asset (assoc units to to-units)])
       (into {})))


(defn reset-assets
  "Resets assets to just their original number of units"
  [assets]
  (->> (for [[asset units] assets]
         [asset {asset (units asset)}])
       (into {})))
