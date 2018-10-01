(ns scrooge.parse-test
  (:require [datetime]
            [clj-time.core :as t]
            [midje.sweet :refer :all]
            [scrooge.parse :refer :all]))


(facts
 "parse date"

 (parse-date "2000/01/01")
 => (t/local-date 2000 1 1))


(facts
 "parse posting"

 (parse-posting
  ["2000/01/01" "" "payee" "Expenses:Doodads" "$" "100.00" "" "stuff"])

 => {:date (t/local-date 2000 1 1)
     :payee "payee"
     :account ["Expenses" "Doodads"]
     :note "stuff"
     :commodity "$"
     :amount 100.0})


(facts
 "group postings"

 (group-postings [{:date (t/local-date 2000 1 1)
                   :payee "a"
                   :note "a-note-1"}
                  {:date (t/local-date 2000 1 1)
                   :payee "a"
                   :note "a-note-2"}
                  {:date (t/local-date 2000 1 1)
                   :payee "b"
                   :note "b-note-1"}
                  {:date (t/local-date 2000 1 1)
                   :payee "a"
                   :note "a-note-3"}
                  {:date (t/local-date 2000 1 2)
                   :payee "a"
                   :note "a-note-4"}
                  {:date (t/local-date 2000 1 2)
                   :payee "b"
                   :note "b-note-2"}])

 => [{:date (t/local-date 2000 1 1)
      :payee "a"
      :postings [{:note "a-note-1"}
                 {:note "a-note-2"}]}
     {:date (t/local-date 2000 1 1)
      :payee "b"
      :postings [{:note "b-note-1"}]}
     {:date (t/local-date 2000 1 1)
      :payee "a"
      :postings [{:note "a-note-3"}]}
     {:date (t/local-date 2000 1 2)
      :payee "a"
      :postings [{:note "a-note-4"}]}
     {:date (t/local-date 2000 1 2)
      :payee "b"
      :postings [{:note "b-note-2"}]}])


(facts
 "dollar map"

 (->dollar-map
  [["BTC" "EUR" 500.0]
   ["$" "INR" 50.0]
   ["EUR" "INR" 75.0]])
 => {"$" 1.0
     "BTC" 750.0
     "EUR" 1.50
     "INR" 0.02})


(facts
 "extract prices"

 (extract-prices
  (str "P 2016/09/17 09:28:53 ₹ $ 0.014900\n"
       "P 2016/09/17 09:28:56 € $ 1.115800"))
 => [["₹" "$" 0.014900]
     ["€" "$" 1.115800]])
