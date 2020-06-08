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
 "convert assets"

 (let [prices {"$" 1.0 "BTC" 500.0 "S&P" 100.0 "NIFTY" 10.0}]

   (convert-assets {"BTC" 10.0 "S&P" 100.0 "NIFTY" 10.0}
                   prices
                   "$")
   => {"BTC" 5000.0
       "S&P" 10000.0
       "NIFTY" 100.0}))


(facts
 "rebalance keep"
 (#'scrooge.assets/rebalance-keep 0.0 {"BTC" 10.0})
 => {}

 (#'scrooge.assets/rebalance-keep 10.0 {})
 => {}

 (#'scrooge.assets/rebalance-keep 10.0 {"BTC" -5.0})
 => {}

 (#'scrooge.assets/rebalance-keep 10.0 {"BTC" 5.0})
 => {"BTC" 5.0}

 (#'scrooge.assets/rebalance-keep 10.0 {"BTC" 15.0 "S&P" 15.0})
 => {"BTC" 5.0 "S&P" 5.0}

 (#'scrooge.assets/rebalance-keep 5.0 {"BTC" 15.0 "S&P" 10.0})
 => {"BTC" 5.0}

 (#'scrooge.assets/rebalance-keep 10.0 {"BTC" 20.0 "S&P" 20.0 "NIFTY" 10.0})
 => {"BTC" 5.0 "S&P" 5.0}

 (#'scrooge.assets/rebalance-keep 15.0 {"BTC" 15.0 "S&P" 10.0})
 => {"BTC" 10.0 "S&P" 5.0}

 (#'scrooge.assets/rebalance-keep 50.0 {"BTC" 15.0 "S&P" 10.0})
 => {"BTC" 15.0 "S&P" 10.0})


(facts
 "rebalance assets"

 (rebalance-assets {"BTC" 10.0 "S&P" 10.0}
                   {"$" 1.0 "BTC" 500.0 "S&P" 100.0}
                   {"BTC" 0.5 "S&P" 0.5}
                   {"$" 1000.0}
                   :sell? true)
 => {"BTC" -3.0
     "S&P" 25.0}

 (rebalance-assets {"BTC" 10.0 "S&P" 10.0}
                   {"$" 1.0 "BTC" 500.0 "S&P" 100.0}
                   {"BTC" 0.5 "S&P" 0.5}
                   {"$" 1000.0})
 => {"S&P" 10.0})
