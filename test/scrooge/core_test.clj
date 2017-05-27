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
 "subaccounts"

 (subaccounts {["Assets" "Wallet"] {"$" 10.0}
               ["Expenses" "Restaurants"] {"$" 5.0}}
              "assets")
 => {["Assets" "Wallet"] {"$" 10.0}}


 (subaccounts {["Assets" "Wallet"] {"$" 10.0}
               ["Expenses" "Restaurants"] {"$" 5.0}}
              #"set")
 => {}


 (subaccounts {["Assets" "Wallet"] {"$" 10.0}
               ["Expenses" "Restaurants"] {"$" 5.0}}
              #"W.+t")
 => {["Assets" "Wallet"] {"$" 10.0}})
