(ns scrooge.core-test
  (:require [midje.sweet :refer :all]
            [scrooge.core :refer :all]
            [clj-time.core :as t]))


(facts
 "convert"

 (let [prices {"$" 1.0
               "INR" 0.02
               "BTC" 750.0}]

   (convert-amount prices "BTC" "INR" 1.0)
   => 37500.0

   (convert-amount prices "BTC" "INR" 2.0)
   => 75000.0))

;;; postings

(facts
 "between"

 (between [{:date (t/local-date 2000 1 1)}
           {:date (t/local-date 2001 1 1)}
           {:date (t/local-date 2001 1 2)}
           {:date (t/local-date 2010 1 1)}]
          :from (t/local-date 2001 1 1)
          :to (t/local-date 2001 1 2))
 => [{:date (t/local-date 2001 1 1)}])


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

;;; accounts


(facts
 "convert accounts"

 (convert-accounts {["Assets"] {"$" 10.0 "BTC" 1.0}
                    ["Liabilities"] {"$" -10.0}}
                   {"$" 1.0 "BTC" 750.0}
                   "$")
 => {["Assets"] {"$" 760.0}
     ["Liabilities"] {"$" -10.0}})


(facts
 "aggregate"

 (let [accounts {["Assets" "Wallet"] {"$" 10.0
                                      "BTC" 1.0}
                 ["Expenses" "Eating Out" "Coffee Shops"] {"$" 100.0}
                 ["Expenses" "Eating Out" "Restaurants"] {"$" 100.0}
                 ["Expenses" "Taxes"] {"$" 100.0}}]

   (aggregate accounts 0)
   => {["Assets"] {"$" 10.0 "BTC" 1.0}
       ["Expenses"] {"$" 300.0}}

   (aggregate accounts 1)
   => {["Assets" "Wallet"] {"$" 10.0 "BTC" 1.0}
       ["Expenses" "Eating Out"] {"$" 200.0}
       ["Expenses" "Taxes"] {"$" 100.0}}

   (aggregate accounts 2)
   => accounts))


(facts
 "match"

 (let [accounts {["Assets" "Wallet"] {"$" 10.0
                                      "BTC" 1.0}
                 ["Expenses" "Eating Out" "Coffee Shops"] {"$" 100.0}
                 ["Expenses" "Eating Out" "Restaurants"] {"$" 100.0}
                 ["Expenses" "Taxes"] {"$" 100.0}}]

   (match accounts "Assets")
   => {["Assets" "Wallet"] {"$" 10.0
                            "BTC" 1.0}}

   (match accounts "Eating Out")
   => {["Expenses" "Eating Out" "Coffee Shops"] {"$" 100.0}
       ["Expenses" "Eating Out" "Restaurants"] {"$" 100.0}}))


(facts
 "fractional"

 (let [accounts {["Assets" "Wallet"] {"BTC" 1.0}
                 ["Expenses" "Eating Out" "Coffee Shops"] {"$" 100.0}
                 ["Expenses" "Eating Out" "Restaurants"] {"$" 100.0}}]

   (fractional accounts {"$" 1.0 "BTC" 800.0})
   => {["Assets" "Wallet"] {"BTC" 0.8}
       ["Expenses" "Eating Out" "Coffee Shops"] {"$" 0.1}
       ["Expenses" "Eating Out" "Restaurants"] {"$" 0.1}}

   (fractional accounts {"$" 1.0 "BTC" 800.0} :tolerance 0.1)
   => {["Assets" "Wallet"] {"BTC" 0.8}}))



(facts
 "delta"

 (delta {["Expenses" "Groceries"] {"$" 10.0}
         ["Expenses" "Eating Out" "Coffee Shops"] {"$" 20.0}
         ["Expenses" "Eating Out" "Restaurants"] {"$" 10.0}}
        {["Expenses" "Eating Out" "Coffee Shops"] {"$" 10.0}
         ["Expenses" "Eating Out" "Restaurants"] {"$" 20.0}
         ["Expenses" "Travel"] {"$" 10.0}})
 => {["Expenses" "Groceries"] {"$" 10.0}
     ["Expenses" "Eating Out" "Coffee Shops"] {"$" 10.0}
     ["Expenses" "Eating Out" "Restaurants"] {"$" -10.0}
     ["Expenses" "Travel"] {"$" -10.0}})
