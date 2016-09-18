(ns scrooge.parse-test
  (:require [datetime]
            [clj-time.core :refer [local-date]]
            [midje.sweet :refer :all]
            [scrooge.parse :refer :all])
  (:import [java.io StringReader]))


(facts
 "dollar map"

 (->dollar-map
  [["BTC" "EUR" 500.0]
   ["$" "INR" 50.0]
   ["EUR" "INR" 75.0]])
 => {"$" 1.0
     "BTC" 750.0
     "EUR" 1.50
     "INR" 0.02})


(facts
 "extract prices"

 (extract-prices
  (str "P 2016/09/17 09:28:53 ₹ $ 0.014900\n"
       "P 2016/09/17 09:28:56 € $ 1.115800"))
 => [["₹" "$" 0.014900]
     ["€" "$" 1.115800]])
