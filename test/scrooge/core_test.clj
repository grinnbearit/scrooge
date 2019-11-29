(ns scrooge.core-test
  (:require [midje.sweet :refer :all]
            [scrooge.core :refer :all]
            [clj-time.core :as t]
            [scrooge.data-readers]))


(facts
 "convert"

 (let [prices {"$" 1.0
               "INR" 0.02
               "BTC" 750.0}]

   (convert-amount prices "BTC" "INR" 1.0)
   => 37500.0

   (convert-amount prices "BTC" "INR" 2.0)
   => 75000.0))

;;; transactions

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
 "posting matchables"

 (#'scrooge.core/posting-matchables
  {:note nil :account ["Expenses" "Groceries"]})
 => [[:account "Expenses"]
     [:account "Groceries"]]


 (#'scrooge.core/posting-matchables
  {:note "note" :account ["Expenses" "Groceries"]})
 => [[:note "note"]
     [:account "Expenses"]
     [:account "Groceries"]])


(facts
 "transaction matchables"

 (#'scrooge.core/transaction-matchables
  {:note nil :payee "payee" :postings [:postings-1
                                       :postings-2]})
 => [[:payee "payee"]
     [:note "posting-1"]
     [:account "account-1"]
     [:note "posting-2"]
     [:account "account-2"]]

 (provided
  (posting-matchables :postings-1)
  => [[:note "posting-1"]
      [:account "account-1"]]

  (posting-matchables :postings-2)
  => [[:note "posting-2"]
      [:account "account-2"]])


 (#'scrooge.core/transaction-matchables
  {:note "note" :payee "payee" :postings []})

 => [[:note "note"]
     [:payee "payee"]])


(facts
 "match"

 (let [ledger [{:note "Quarterly Interest" :payee "Big Bank"
                :postings [{:note nil :account ["Income" "Interest"]}
                           {:note nil :account ["Assets" "Savings" "Bank"]}]}
               {:note nil :payee "Small Store"
                :postings [{:note "Milk" :account ["Expenses" "Groceries"]}
                           {:note "Eggs" :account ["Expenses" "Groceries"]}
                           {:note nil :account ["Assets" "Savings" "Wallet"]}]}]]

   ;; text match
   (match ledger "groceries")
   => [(ledger 1)]


   ;; regex match
   (match ledger #"groceries")
   => []


   ;; regex match case insensitive
   (match ledger #"(?i)groceries")
   => [(ledger 1)]


   ;; match both entries
   (match ledger "savings")
   => ledger

   ;; match both entries
   (match ledger #"(Big|Milk).*")
   => ledger

   (match ledger #"(Big|Milk).*" :include #{:payee})
   => [(ledger 0)]

   (match ledger #"(Big|Milk).*" :exclude #{:payee})
   => [(ledger 1)]))


(facts
 "balance"

 (balance [{:postings [{:account ["Expenses"] :commodity "$" :amount 10.0}
                       {:account ["Wallet"], :commodity "$", :amount -10.0}]}
           {:postings [{:account ["Expenses"] :commodity "BTC" :amount 1.0}
                       {:account ["Wallet"], :commodity "BTC", :amount -1.0}
                       {:account ["Expenses"] :commodity "$" :amount 1.0}
                       {:account ["Wallet"], :commodity "$", :amount -1.0}]}])
 => {["Expenses"] {"$" 11.0
                   "BTC" 1.0}
     ["Wallet"] {"$" -11.0
                 "BTC" -1.0}})


(facts
 "aggregate daily postings"

 (#'scrooge.core/aggregate-daily-postings
  [{:date (t/local-date 2000 1 1)
    :postings [{:account ["Exchange"] :commodity "BTC" :amount 1.0 :price 1000 :unit "$"}
               {:account ["Wallet"] :commodity "$" :amount -1000 :price nil :unit nil}]}
   {:date (t/local-date 2000 1 1)
    :postings [{:account ["Exchange"] :commodity "BTC" :amount 0.5 :price 910 :unit "€"}
               {:account ["Exchange"] :commodity "BTC" :amount 0.5 :price 900 :unit "€"}
               {:account ["Wallet"] :commodity "€" :amount -905 :price nil :unit nil}]}
   {:date (t/local-date 2000 1 1)
    :postings [{:account ["Exchange"] :commodity "BTC" :amount 1.0 :price nil :unit nil}
               {:account ["Wallet"] :commodity "BTC" :amount -1.0 :price nil :unit nil}]}
   {:date (t/local-date 2000 1 2)
    :postings [{:account ["Exchange"] :commodity "BTC" :amount 1.0 :price 1100 :unit "$"}
               {:account ["Wallet"] :commodity "$" :amount -1100 :price nil :unit nil}]}])

 => [{:date (t/local-date 2000 1 1)
      :postings {["Exchange"] {"BTC" {nil [{:amount 1.0 :price nil}]
                                      "$" [{:amount 1.0 :price 1000}]
                                      "€" [{:amount 0.5 :price 910}
                                           {:amount 0.5 :price 900}]}}
                 ["Wallet"] {"$" {nil [{:amount -1000 :price nil}]}
                             "BTC" {nil [{:amount -1.0 :price nil}]}
                             "€" {nil [{:amount -905 :price nil}]}}}}

     {:date (t/local-date 2000 1 2)
      :postings {["Exchange"] {"BTC" {"$" [{:amount 1.0 :price 1100}]}}
                 ["Wallet"] {"$" {nil [{:amount -1100 :price nil}]}}}}])


(facts
 "combine daily postings"

 (#'scrooge.core/combine-daily-postings
  [{:date (t/local-date 2000 1 1)
    :postings {["Exchange"] {"BTC" {nil [{:amount 1.0 :price nil}]
                                    "$" [{:amount 1.0 :price 1000}]
                                    "€" [{:amount 0.5 :price 910}
                                         {:amount 0.5 :price 900}]}}
               ["Wallet"] {"$" {nil [{:amount -1000 :price nil}]}
                           "BTC" {nil [{:amount -1.0 :price nil}]}
                           "€" {nil [{:amount -905 :price nil}]}}}}

   {:date (t/local-date 2000 1 2)
    :postings {["Exchange"] {"BTC" {"$" [{:amount 1.0 :price 1100}]}}
               ["Wallet"] {"$" {nil [{:amount -1100 :price nil}]}}}}])

 => [{:date (t/local-date 2000 1 1)
      :balance {["Exchange"] {"BTC" {nil {:amount 1.0 :price nil}
                                     "$" {:amount 1.0 :price 1000.0}
                                     "€" {:amount 1.0 :price 905.0}}}
                ["Wallet"] {"$" {nil {:amount -1000 :price nil}}
                            "BTC" {nil {:amount -1.0 :price nil}}
                            "€" {nil {:amount -905 :price nil}}}}}
     {:date (t/local-date 2000 1 2)
      :balance {["Exchange"] {"BTC" {"$" {:amount 1.0 :price 1100.0}}}
                ["Wallet"] {"$" {nil {:amount -1100 :price nil}}}}}])


(facts
 "daily balance"

 (daily-balance "ledger")
 => "daily-balance"

 (provided
  (#'scrooge.core/aggregate-daily-postings "ledger") => "daily-postings"
  (#'scrooge.core/combine-daily-postings "daily-postings") => "daily-balance"))

;;; accounts


(facts
 "convert balance"

 (#'scrooge.core/convert-balance {"$" 10.0 "BTC" 1.0}
                                 {"$" 1.0 "BTC" 750.0}
                                 "$")
 => {"$" 760.0})


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


(facts
 "subaccount?"

 (subaccount? ["Expenses" "Eating Out" "Restaurants"] ["Expenses" "Eating Out" "Restaurants" "Chinese"])
 => false

 (subaccount? ["Expenses" "Eating Out" "Restaurants"] ["Expenses" "Eating Out" "Restaurants"])
 => true

 (subaccount? ["Expenses" "Eating Out" "Restaurants"] ["Eating Out" "Restaurants"])
 => true

 (subaccount? ["Expenses" "Eating Out" "Restaurants"] [])
 => true)


(facts
 "subaccounts"

 (let [accounts {["Expenses" "Eating Out" "Restaurants"] {"$" 10.0}
                 ["Expenses" "Eating Out" "Snacks"] {"$" 10.0}
                 ["Assets" "Wallet"] {"$" -20.0}}]

   (subaccounts accounts [["Eating Out"]])
   => {["Expenses" "Eating Out" "Restaurants"] {"$" 10.0}
       ["Expenses" "Eating Out" "Snacks"] {"$" 10.0}}

   (subaccounts accounts [["Restaurants"] ["Snacks"]])
   => {["Expenses" "Eating Out" "Restaurants"] {"$" 10.0}
       ["Expenses" "Eating Out" "Snacks"] {"$" 10.0}}))
