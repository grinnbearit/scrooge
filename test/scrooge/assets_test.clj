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
 => {"$" {"$" 25.0}
     "BTC" {"BTC" 1.0}})


(facts
 "convert assets"

 (let [prices {"$" 1.0 "BTC" 100.0 "INR" 1/2}]

   (convert-assets {"$" {"$" 10.0}
                    "BTC" {"BTC" 2.0}}
                   prices
                   "$")
   => {"$" {"$" 10.0}
       "BTC" {"$" 200.0
              "BTC" 2.0}}

   (provided
    (convert-amount prices "$" "$" 10.0) => 10.0
    (convert-amount prices "BTC" "$" 2.0) => 200.0)))


(facts
 "reset assets"

 (reset-assets {"$" {"$" 10.0 "BTC" 1/1000}
                "BTC" {"$" 200.0 "BTC" 2.0}})
 => {"$" {"$" 10.0}
     "BTC" {"BTC" 2.0}})
