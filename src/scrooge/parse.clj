(ns scrooge.parse
  (:require [swissknife.collections :refer [queue]]
            [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [clojure.data.zip.xml :refer [xml-> xml1-> text]]
            [clojure.string :as str]
            [clj-time.core :as t]))


(defn parse-date
  "returnes a joda localdate since time isn't relevant"
  [date-str]
  (let [[year month day] (str/split date-str #"/")]
    (t/local-date (Integer/parseInt year)
                  (Integer/parseInt month)
                  (Integer/parseInt day))))


(defn extract-posting
  "Extracts a transaction posting from a posting xml node"
  [node]
  (let [note (xml1-> node :note text)
        account (xml1-> node :account :name text)
        commodity (xml1-> node :post-amount :amount :commodity :symbol text)
        amount (xml1-> node :post-amount :amount :quantity text)]
    {:account (str/split account #":")
     :note note
     :commodity commodity
     :amount (Double/parseDouble amount)}))


(defn extract-transaction
  "Extracts a complete transaction from a transaction xml node"
  [node]
  (let [date-str (xml1-> node :date text)
        note (xml1-> node :note text)
        payee (xml1-> node :payee text)
        postings (mapv extract-posting
                       (xml-> node :postings :posting))]
    {:date (parse-date date-str)
     :note note
     :payee payee
     :postings postings}))


(defn extract-ledger
  "Extracts a vector of transactions from the ledger xml"
  [ledger-xml]
  (mapv extract-transaction
        (xml-> ledger-xml :transactions :transaction)))


(defn parse-ledger
  "Parses a ledger-cli xml file into a vector of transactions"
  [input]
  (->> (parse input)
       (xml-zip)
       (extract-ledger)))


(defn ->dollar-map
  "Returns a map of the price of a unit of every commodity to USD"
  [prices-seq]
  (loop [acc {"$" 1.0}
         price-queue (apply queue prices-seq)]
    (if (empty? price-queue)
      acc
      (let [[comm unit price] (peek price-queue)]
        (cond (and (acc comm) (acc unit))
              (recur acc (pop price-queue))

              (acc comm)
              (recur (assoc acc unit (/ (acc comm) price))
                     (pop price-queue))

              (acc unit)
              (recur (assoc acc comm (* (acc unit) price))
                     (pop price-queue))

              :else
              (recur acc (conj (pop price-queue)
                               (peek price-queue))))))))


(defn extract-prices
  "extracts a prices from a pricedb string and returns a sequence of prices"
  [text]
  (for [line (str/split text #"\n")
        :let [[_ _ _ commodity unit price] (str/split line #" ")]]
    [commodity unit (Double/parseDouble price)]))


(defn parse-pricedb
  "Parses a pricedb file and returns a dollar-map"
  [input]
  (->> (slurp input)
       (extract-prices)
       (->dollar-map)))
