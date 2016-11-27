(ns scrooge.core-test
  (:require [midje.sweet :refer :all]
            [scrooge.core :refer :all]
            [clj-time.core :as t]))


(facts
 "postings"

 (postings [{:date (t/local-date 2000 1 1)}
            {:date (t/local-date 2001 1 1)}
            {:date (t/local-date 2001 1 2)}
            {:date (t/local-date 2010 1 1)}]
           :from (t/local-date 2001 1 1)
           :to (t/local-date 2001 1 2))
 => [{:date (t/local-date 2001 1 1)}])


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
 "aggregate"

 (let [bal {["Assets" "Wallet"] {"$" 10.0
                                 "BTC" 1.0}
            ["Expenses" "Eating Out" "Coffee Shops"] {"$" 100.0}
            ["Expenses" "Eating Out" "Restaurants"] {"$" 100.0}
            ["Expenses" "Taxes"] {"$" 100.0}}]

   (aggregate bal 0)
   => {["Assets"] {"$" 10.0 "BTC" 1.0}
       ["Expenses"] {"$" 300.0}}

   (aggregate bal 1)
   => {["Assets" "Wallet"] {"$" 10.0 "BTC" 1.0}
       ["Expenses" "Eating Out"] {"$" 200.0}
       ["Expenses" "Taxes"] {"$" 100.0}}

   (aggregate bal 2)
   => bal))


(facts
 "match"

 (let [bal {["Assets" "Wallet"] {"$" 10.0
                                 "BTC" 1.0}
            ["Expenses" "Eating Out" "Coffee Shops"] {"$" 100.0}
            ["Expenses" "Eating Out" "Restaurants"] {"$" 100.0}
            ["Expenses" "Taxes"] {"$" 100.0}}]

   (match bal "Assets")
   => {["Assets" "Wallet"] {"$" 10.0
                            "BTC" 1.0}}

   (match bal "Eating Out")
   => {["Expenses" "Eating Out" "Coffee Shops"] {"$" 100.0}
       ["Expenses" "Eating Out" "Restaurants"] {"$" 100.0}}))


(facts
 "net worth"

 (net-worth {["Assets" "Wallet"] {"$" 10.0
                                  "BTC" 1.0}
             ["Assets" "Bank"] {"$" 100.0}
             ["Liabilities" "Credit Card"] {"$" -100.0}})
 => {["Assets"] {"$" 110.0
                 "BTC" 1.0}
     ["Liabilities"] {"$" -100.0}})


(facts
 "portfolio"

 (portfolio {["Assets" "Wallet"] {"$" 100 "BTC" 1}
             ["Assets" "Bank"] {"$" 100}
             ["Assets" "Sofa"] {"$" 1}
             ["Liabilities" "Credit Card"] {"$" -100}}
            {"$" 1 "BTC" 100}
            :tolerance 1/100)
 => {"Assets" {"Bank" {"$" 100/301},
               "Wallet" {"$" 100/301,
                         "BTC" 100/301}}})


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
