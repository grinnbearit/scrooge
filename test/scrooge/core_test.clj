(ns scrooge.core-test
  (:require [midje.sweet :refer :all]
            [scrooge.core :refer :all]))


(facts
 "convert"

 (let [dollar-map {"$" 1.0
                   "INR" 0.02
                   "BTC" 750.0}]

   (convert-commodities dollar-map "BTC" "INR")
   => 37500.0

   (convert-commodities dollar-map "BTC" "INR" 2.0)
   => 75000.0))


(facts
 "balance"

 (balance [{:postings [{:account "Expenses" :commodity "$" :amount 10.0}
                       {:account "Wallet", :commodity "$", :amount -10.0}]}
           {:postings [{:account "Expenses" :commodity "BTC" :amount 1.0}
                       {:account "Wallet", :commodity "BTC", :amount -1.0}]}])
 => {"Expenses" {"$" 10.0
                 "BTC" 1.0}
     "Wallet" {"$" -10.0
               "BTC" -1.0}})
