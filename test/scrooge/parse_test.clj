(ns scrooge.parse-test
  (:require [midje.sweet :refer :all]
            [clojure.spec.alpha :as s]
            [expound.alpha :as e]
            [orchestra.spec.test :as o]
            [clojure.spec.gen.alpha :as gen]
            [scrooge.parse :refer :all]
            [clj-time.core :as t]
            [clojure.java.io :as io]))



(set! s/*explain-out* e/printer)
(o/instrument)


(facts "about `is-amount?`"

       (fact "returns true if the string looks like a number (with commas)"
             (is-amount? "123,456.00") => true)

       (fact "returns false if the string doesn't look like a number"
             (is-amount? "not-a-number") => false)

       (fact "returns false if the string has commas in the wrong places"
             (is-amount? "123,456,00") => false))


(facts "about `parse-amount`"

       (fact "returns a double if given a number as a string"
             (parse-amount "100") => 100.0)

       (fact "removes commas before parsing"
             (parse-amount "1,000.50") => 1000.50))


(facts "about `parse-posting`"

       (fact "returns a scrooge/posting without a price"
             (parse-posting `(nil "Expenses:Doodads" "INR 1,000.0" nil "note"))
             => {:scrooge/account ["Expenses" "Doodads"]
                 :scrooge/amount 1000.0
                 :scrooge/commodity "INR"
                 :scrooge/note "note"})

       (fact "returns a scrooge/posting with a price"
             (parse-posting  `(nil "Expenses:Doodads" "10.0 BTC {INR 1,000.0} [2000-01-01]" nil "note"))
             => {:scrooge/account ["Expenses" "Doodads"]
                 :scrooge/amount 10.0
                 :scrooge/commodity "BTC"
                 :scrooge/price 1000.0
                 :scrooge/unit "INR"
                 :scrooge/note "note"}))


(facts "about `parse-transaction`"
       (let [postings (-> (s/gen :scrooge/posting)
                          (gen/sample 2)
                          vec)]

         (fact "returns a seq of postings"

               (parse-transaction `(nil nil (14445 16772 0) nil "payee" (0) (1)))
               => {:scrooge/date (t/local-date 2000 1 1)
                   :scrooge/payee "payee"
                   :scrooge/postings postings}

               (provided
                (parse-posting `(0)) => (postings 0)
                (parse-posting `(1)) => (postings 1)))))


(facts "about `parse-ledger`"

       (fact "returns a seq of transactions"
             (let [transactions (-> (s/gen :scrooge/transaction)
                                    (gen/sample 2)
                                    vec)]

               (parse-ledger "filename")
               => transactions

               (provided
                (io/reader "filename") => (java.io.StringReader. "((0) (1))")
                (parse-transaction `(0)) => (transactions 0)
                (parse-transaction `(1)) => (transactions 1)))))
