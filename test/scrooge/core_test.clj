(ns scrooge.core-test
  (:require [midje.sweet :refer :all]
            [scrooge.core :refer :all]))


(facts
 "convert"

 (let [dollar-map {"$" 1.0
                   "INR" 0.02
                   "BTC" 750.0}]

   (convert-amount dollar-map "BTC" "INR" 1.0)
   => 37500.0

   (convert-amount dollar-map "BTC" "INR" 2.0)
   => 75000.0))


(facts
 "balance"

 (balance [{:postings [{:account ["Expenses"] :commodity "$" :amount 10.0}
                       {:account ["Wallet"], :commodity "$", :amount -10.0}]}
           {:postings [{:account ["Expenses"] :commodity "BTC" :amount 1.0}
                       {:account ["Wallet"], :commodity "BTC", :amount -1.0}]}])
 => {["Expenses"] {"$" 10.0
                   "BTC" 1.0}
     ["Wallet"] {"$" -10.0
                 "BTC" -1.0}})


(facts
 "convert accounts"

 (convert-accounts {["Assets"] {"$" 10.0 "BTC" 1.0}
                    ["Liabilities"] {"$" -10.0}}
                   {"$" 1.0 "BTC" 750.0}
                   "$")
 => {["Assets"] {"$" 760.0}
     ["Liabilities"] {"$" -10.0}})


(facts
 "net worth"

 (net-worth {["Assets" "Wallet"] {"$" 10.0
                                  "BTC" 1.0}
             ["Assets" "Bank"] {"$" 100.0}
             ["Liabilities" "Credit Card"] {"$" -100.0}})
 => {["Assets"] {"$" 110.0
                 "BTC" 1.0}
     ["Liabilities"] {"$" -100.0}})
