(ns scrooge.parse
  (:require [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [clojure.data.zip.xml :refer [xml-> xml1-> text]]
            [clojure.string :as str]))


(defn extract-posting
  [node]
  (let [note (xml1-> node :note text)
        account (xml1-> node :account :name text)
        commodity (xml1-> node :post-amount :amount :commodity :symbol text)
        amount (xml1-> node :post-amount :amount :quantity text)]
    {:account account
     :note note
     :commodity commodity
     :amount (Double/parseDouble amount)}))


(defn extract-transaction
  [node]
  (let [date-str (xml1-> node :date text)
        note (xml1-> node :note text)
        payee (xml1-> node :payee text)
        postings (mapv extract-posting
                       (xml-> node :postings :posting))]
    {:date date-str
     :note note
     :payee payee
     :postings postings}))


(defn extract-ledger
  [ledger-xml]
  (mapv extract-transaction
        (xml-> ledger-xml :transactions :transaction)))


(defn parse-ledger
  [input]
  (->> (parse input)
       (xml-zip)
       (extract-ledger)))


(defn parse-prices
  [input]
  (for [line (line-seq input)
        :let [[_ date-str _ commodity unit price] (str/split line #" ")]]
    {:date date-str
     :commodity commodity
     :unit unit
     :price (Double/parseDouble price)}))
