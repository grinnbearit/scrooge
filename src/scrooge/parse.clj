(ns scrooge.parse
  (:require [swissknife.collections :refer [queue]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [scrooge.data-readers]))


(defn is-amount?
  "Returns true if the string only consists of digits, ',', and '.'"
  [s]
  (not (nil? (re-matches #"[-\d,\.]+" s))))


(defn parse-amount
  "Returns the parsed amount as a double"
  [s]
  (Double/parseDouble (str/replace s "," "" )))


(defn parse-posting
  [posting]
  (let [[_ account-str amount-str _ note]
        posting

        account
        (str/split account-str #":")

        [_ amt-1 amt-2 _ price-1 price-2]
        (re-matches #"([^\s]+) ([^\s]+)( \{([^\s]+) ([^\s]+)\}.+)?" amount-str)

        [amount commodity]
        (if (is-amount? amt-1)
          [(parse-amount amt-1) amt-2]
          [(parse-amount amt-2) amt-1])

        [price unit]
        (cond (nil? price-1)
              nil

              (is-amount? price-1)
              [(parse-amount price-1) price-2]

              :else
              [(parse-amount price-2) price-1])]

    {:account account
     :amount amount                     ; the amount moved from or into the account
     :commodity commodity               ; the commodity moved
     :price price                       ; the price of the commodity (or nil)
     :unit unit                         ; the unit the price is in
     :note note}))


(defn parse-transaction
  [transaction]
  (let [[_ _ [date_msb date_lsb _] _ payee & postings]
        transaction

        date
        (-> (+ (bit-shift-left date_msb 16) date_lsb)
            (tc/from-epoch)
            (tc/in-time-zone (t/default-time-zone)))]

    {:date date
     :payee payee
     :postings (map parse-posting postings)}))


(defn parse-ledger
  "Parses a ledger-cli edn file into a vector of transactions"
  [input]
  (with-open [reader (io/reader input)]
    (->> (read (java.io.PushbackReader. reader))
         (map parse-transaction))))


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
