(ns scrooge.core)


(defn convert-commodities
  "Given a dollar-map, converts any 2 commodities"
  ([dollar-map from to]
   (convert-commodities dollar-map from to 1.0))
  ([dollar-map from to amount]
   (/ (* amount (dollar-map from))
      (dollar-map to))))
