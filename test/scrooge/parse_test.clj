(ns scrooge.parse-test
  (:require [midje.sweet :refer :all]
            [scrooge.parse :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]))


(facts
 "is amount?"

 (is-amount? "100")
 => true

 (is-amount? "100.0")
 => true

 (is-amount? "-1,000.0")
 => true

 (is-amount? "1.00.00,00")
 => true

 (is-amount? "1a")
 => false)


(facts
 "parse amount"

 (parse-amount "100")
 => 100.0

 (parse-amount "1,000,000.00")
 => 1000000.00)


(facts
 "parse posting"

 (parse-posting `(nil "Expenses:Doodads" "INR 1,000.0" nil "note"))
 => {:account ["Expenses" "Doodads"]
     :amount 1000.0
     :commodity "INR"
     :price nil
     :unit nil
     :note "note"}


 (parse-posting `(nil "Expenses:Doodads" "10.0 BTC {INR 1,000.0} [2000-01-01]" nil "note"))
 => {:account ["Expenses" "Doodads"]
     :amount 10.0
     :commodity "BTC"
     :price 1000.0
     :unit "INR"
     :note "note"})


(facts
 "parse transaction"

 (parse-transaction '(nil nil (14445 16772 0) nil "payee" "posting-1" "posting-2"))
 => {:date (t/local-date 2000 1 1)
     :payee "payee"
     :postings ["parsed-posting-1" "parsed-posting-2"]}

 (provided
  (parse-posting "posting-1") => "parsed-posting-1"
  (parse-posting "posting-2") => "parsed-posting-2"))


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
