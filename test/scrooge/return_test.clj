(ns scrooge.return-test
  (:require [midje.sweet :refer :all]
            [scrooge.return :refer :all]
            [clj-time.core :as t]
            [scrooge.data-readers]))


(facts
 "aggregate daily postings"

 (#'scrooge.return/aggregate-daily-postings
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

 (#'scrooge.return/combine-daily-postings
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
  (#'scrooge.return/aggregate-daily-postings "ledger") => "daily-postings"
  (#'scrooge.return/combine-daily-postings "daily-postings") => "daily-balance"))
