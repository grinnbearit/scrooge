(ns scrooge.parse
  (:require [swissknife.collections :refer [queue]]
            [clojure.data.csv :as csv]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-time.core :as t]))


(defn parse-date
  "returnes a joda localdate since time isn't relevant"
  [date-str]
  (let [[year month day] (str/split date-str #"/")]
    (t/local-date (Integer/parseInt year)
                  (Integer/parseInt month)
                  (Integer/parseInt day))))


(defn parse-posting
  [[date-str _ payee account commodity amount _ note]]
  {:date (parse-date date-str)
   :payee payee
   :account (str/split account #":")
   :note note
   :commodity commodity
   :amount (Double/parseDouble amount)})


(defn group-postings
  "Extracts a transaction posting from a posting xml node"
  [postings]
  (letfn [(reducer [acc group]
            (let [transaction (select-keys (first group) [:date :payee])]
              (->> (mapv #(dissoc % :date :payee) group)
                   (assoc transaction :postings)
                   (conj acc))))]

    (->> (partition-by #(select-keys % [:date :payee]) postings)
         (reduce reducer []))))


(defn parse-ledger
  "Parses a ledger-cli csv file into a vector of transactions"
  [input]
  (with-open [reader (io/reader input)]
    (->> (csv/read-csv reader)
         (map parse-posting)
         (group-postings))))


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
