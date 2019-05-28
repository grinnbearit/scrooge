(ns scrooge.parse
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [orchestra.core :refer [defn-spec]]
            [clj-time.spec])
  (:import [java.io PushbackReader]))


(s/def :scrooge/date :clj-time.spec/local-date)
(s/def :scrooge/payee string?)
(s/def :scrooge/account (s/coll-of string? :kind? vector? :min-count 1))
(s/def :scrooge/amount double?)
(s/def :scrooge/commodity string?)
(s/def :scrooge/price double?)
(s/def :scrooge/unit string?)
(s/def :scrooge/note string?)
(s/def :scrooge/posting (s/keys :req [:scrooge/account
                                      :scrooge/amount
                                      :scrooge/commodity]
                                :opt [:scrooge/price
                                      :scrooge/unit
                                      :scrooge/note]))
(s/def :scrooge/postings (s/coll-of :scrooge/posting))
(s/def :scrooge/transaction (s/keys :req [:scrooge/date
                                          :scrooge/payee
                                          :scrooge/postings]))



(defn-spec is-amount? boolean?
  "Returns true if the string matches a number with ',' in the thousands places"
  [s string?]
  (not (nil? (re-matches #"^\d{1,3}(,\d{3})*(\.\d+)?$" s))))


(defn-spec parse-amount double?
  "Returns the parsed amount as a double"
  [s string?]
  (Double/parseDouble (str/replace s "," "")))


(defn-spec parse-posting :scrooge/posting
  "Given a ledger lisp sexp, returns a parsed scrooge/posting"
  [sexp seq?]
  (let [[_ account-str amount-str _ note]
        sexp

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

    (cond-> {:scrooge/account account
             :scrooge/note note
             :scrooge/amount amount        ; the amount moved from or into the account
             :scrooge/commodity commodity} ; the commodity moved
      price (assoc :scrooge/price price    ; the price of the commodity
                   :scrooge/unit unit))))  ; the unit the price is in


(defn-spec parse-transaction :scrooge/transaction
  [sexp seq?]
  (let [[_ _ [date_msb date_lsb _] _ payee & postings]
        sexp

        date
        (-> (+ (bit-shift-left date_msb 16) date_lsb)
            (tc/from-epoch)
            (tc/in-time-zone (t/default-time-zone)))]

    {:scrooge/date date
     :scrooge/payee payee
     :scrooge/postings (map parse-posting postings)}))


(defn-spec parse-ledger (s/coll-of :scrooge/transaction)
  "given an input source returns a parsed ledger"
  [input string?]
  (with-open [reader (PushbackReader. (io/reader input))]
    (->> (read reader)
         (map parse-transaction))))
