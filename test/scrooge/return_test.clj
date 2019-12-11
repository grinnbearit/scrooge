(ns scrooge.return-test
  (:require [midje.sweet :refer :all]
            [scrooge.return :refer :all]
            [clj-time.core :as t]
            [scrooge.data-readers]))


(facts
 "ledger -> daily postings"

 (ledger->daily-postings
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
 "aggregate daily postings"

 (let [daily-postings [{:date (t/local-date 2000 1 1)
                        :postings {["Assets" "Exchange"] {"BTC" {nil [{:amount 1.0 :price nil}]
                                                                 "$" [{:amount 1.0 :price 1000}]
                                                                 "€" [{:amount 0.5 :price 910}
                                                                      {:amount 0.5 :price 900}]}}
                                   ["Assets" "Wallet"] {"$" {nil [{:amount -1000 :price nil}]}
                                                        "BTC" {nil [{:amount -1.0 :price nil}]}
                                                        "€" {nil [{:amount -905 :price nil}]}}}}

                       {:date (t/local-date 2000 1 2)
                        :postings {["Assets" "Exchange"] {"BTC" {"$" [{:amount 1.0 :price 1100}]}}
                                   ["Assets" "Wallet"] {"$" {nil [{:amount -1100 :price nil}]}}}}]]

   (aggregate-daily-postings daily-postings 0)
   => [{:date (t/local-date 2000 1 1)
        :postings {["Assets"] {"$" {nil [{:amount -1000 :price nil}]}
                               "BTC" {nil [{:amount 1.0 :price nil}
                                           {:amount -1.0 :price nil}]
                                      "$" [{:amount 1.0 :price 1000}]
                                      "€" [{:amount 0.5 :price 910}
                                           {:amount 0.5 :price 900}]}
                               "€" {nil [{:amount -905 :price nil}]}}}}
       {:date (t/local-date 2000 1 2)
        :postings {["Assets"] {"$" {nil [{:amount -1100 :price nil}]}
                               "BTC" {"$" [{:amount 1.0 :price 1100}]}}}}]


   (aggregate-daily-postings daily-postings 1)
   => daily-postings


   (aggregate-daily-postings daily-postings 2)
   => daily-postings))


(facts
 "daily subaccounts"

 (daily-subaccounts [{:date (t/local-date 2000 1 1)
                      :postings {["Assets" "Wallet"] {"$" {nil [{:amount -1000 :price nil}
                                                                {:amount -100 :price nil}]}}
                                 ["Assets" "Bank"] {"$" {nil [{:amount 1000 :price nil}]}}
                                 ["Expenses" "Groceries"] {"$" {nil [{:amount 100 :price nil}]}}}}
                     {:date (t/local-date 2000 1 2)
                      :postings {["Assets" "Wallet"] {"$" {nil [{:amount -200 :price nil}]}}
                                 ["Liabilities" "Bills"] {"$" {nil [{:amount 200 :price nil}]}}}}]
                    [["Assets"] ["Liabilities"]])

 => [{:date (t/local-date 2000 1 1)
      :postings {["Assets" "Bank"] {"$" {nil [{:amount 1000 :price nil}]}}
                 ["Assets" "Wallet"] {"$" {nil [{:amount -1000 :price nil}
                                                {:amount -100 :price nil}]}}}}
     {:date (t/local-date 2000 1 2)
      :postings {["Assets" "Wallet"] {"$" {nil [{:amount -200 :price nil}]}}
                 ["Liabilities" "Bills"] {"$" {nil [{:amount 200 :price nil}]}}}}])


(facts
 "daily-postings -> daily-balance"

 (daily-postings->daily-balance
  [{:date (t/local-date 2000 1 1)
    :postings {["Assets"] {"$" {nil [{:amount -1000 :price nil}]}
                           "BTC" {nil [{:amount 1.0 :price nil}
                                       {:amount -1.0 :price nil}]
                                  "$" [{:amount 1.0 :price 1000}
                                       {:amount -0.6 :price 1200}]
                                  "€" [{:amount 0.5 :price 910}
                                       {:amount 0.5 :price 900}]}
                           "€" {nil [{:amount -905 :price nil}]}}}}
   {:date (t/local-date 2000 1 2)
    :postings {["Assets"] {"$" {nil [{:amount -1100 :price nil}]}
                           "BTC" {"$" [{:amount 1.0 :price 1100}]}}}}])

 => [{:date (t/local-date 2000 1 1)
      :balance {["Assets"] {"$" {nil {:amount -1000 :price nil}}
                            "BTC" {"$" {:amount 0.4 :price 1075.0}
                                   "€" {:amount 1.0 :price 905.0}}
                            "€" {nil {:amount -905 :price nil}}}}}
     {:date (t/local-date 2000 1 2)
      :balance {["Assets"] {"$" {nil {:amount -1100 :price nil}}
                            "BTC" {"$" {:amount 1.0 :price 1100.0}}}}}])
