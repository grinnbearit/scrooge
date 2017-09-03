(ns scrooge.assets-test
  (:require [midje.sweet :refer :all]
            [scrooge.core :refer [convert-amount]]
            [scrooge.assets :refer :all]
            [clj-time.core :as t]))


(facts
 "accounts -> assets"

 (accounts->assets {["A" "i"] {"$" 10.0}
                    ["A" "ii"] {"$" 20.0}
                    ["A" "iii"] {"$" -5.0
                                 "BTC" 1.0}})
 => {"$" 25.0
     "BTC" 1.0})


(facts
 "total value"

 (let [prices {"$" 1.0
               "BTC" 100.0}]

   (total-value {"$" -25.0
                 "BTC" 1.0}
                prices
                "$")
   => 75.0

   (provided
    (convert-amount prices "$" "$" -25.0) => -25.0
    (convert-amount prices "BTC" "$" 1.0) => 100.0)))


(facts
 "rebalance assets"

 (let [prices {"$" 1.0 "BTC" 100.0}]

   (rebalance-assets {"$" 1000.0 "INR" 100.0} prices {"$" 0.5 "BTC" 0.5})
   => {"$" 500.0 "BTC" 5.0}

   (provided
    (total-value {"$" 1000.0 "BTC" 0.0} prices "$") => 1000.0
    (convert-amount prices "$" "$" 500.0) => 500.0
    (convert-amount prices "$" "BTC" 500.0) => 5.0)))
