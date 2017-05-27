(ns scrooge.core
  (:require [clj-time.core :as t]
            [clojure.string :as str]
            [clojure.set :as set]
            [swissknife.core :refer [map-values]]))


(defn convert-amount
  "Given a map of prices, converts any 2 commodities"
  [prices from to amount]
  (/ (* amount (prices from))
     (prices to)))

;;; postings

(defn between
  "Filters ledger entries to those that are
  `from` <= `date` < `to`"
  [ledger & {:keys [from to]}]
  (for [{:keys [date] :as transaction} ledger
        :when (and (or (nil? from)
                       (not (t/before? date from)))
                   (or (nil? to)
                       (t/before? date to)))]
    transaction))


(defmulti match
  "Returns all postings that satisfy matcher

  if pattern is a string, returns all postings that have
  a component that contains it as a case insensitive substring

  if pattern is a regex, returns all postings that satisfy it"
  (fn [ledger pattern & sections]
    (type pattern)))


(defn posting-matchables
  "Returns a list of text pieces from a posting
  that can be matched against"
  [{:keys [note account]}]
  (cond-> (for [acc account]
            [:account acc])
    note (conj [:note note])))


(defn- transaction-matchables
  "Returns a list of text pieces from a transaction
  which can be matched against"
  [{:keys [note payee postings]}]
  (cond-> (conj (mapcat posting-matchables postings)
                [:payee payee])
    note (conj [:note note])))


(defmethod match java.lang.String
  [ledger pattern & {:keys [include exclude]
                     :or {include #{:account :note :payee}
                          exclude #{}}}]
  (let [ptrn (str/lower-case pattern)
        included? (set/difference include exclude)]
    (for [transaction ledger
          :when (some identity
                      (for [[section text] (transaction-matchables transaction)
                            :when (included? section)
                            :let [txt (str/lower-case text)]]
                        (str/includes? txt ptrn)))]
      transaction)))


(defmethod match java.util.regex.Pattern
  [ledger pattern & {:keys [include exclude]
                     :or {include #{:account :note :payee}
                          exclude #{}}}]
  (let [included? (set/difference include exclude)]
    (for [transaction ledger
          :when (some identity
                      (for [[section text] (transaction-matchables transaction)
                            :when (included? section)]
                        (re-matches pattern text)))]
      transaction)))


(defn balance
  "Returns the balance amount in every account"
  [ledger]
  (->> (for [transaction ledger
             {:keys [account commodity amount]} (:postings transaction)]
         {account {commodity amount}})
       (reduce (partial merge-with (partial merge-with +)))))

;;; accounts

(defn convert-accounts
  "Convert all account balances to the same commodity"
  [accounts prices to]
  (->> (for [[account bal] accounts
             [commodity amount] bal]
         {account {to (convert-amount prices commodity to amount)}})
       (reduce (partial merge-with (partial merge-with +)))))


(defn aggregate
  "Given a set of account balances and a level,
  returns all balances aggregated at level"
  [accounts level]
  (->> (for [[[root :as account] bal] accounts
             :let [sub-acc (if (< level (count account))
                             (subvec account 0 (inc level))
                             account)]]
         {sub-acc bal})
       (reduce (partial merge-with (partial merge-with +)))))


(defn fractional
  "Given a set of account balances and a dollar map returns
   fractions of the whole for each account and currency"
  [accounts prices & {:keys [tolerance] :or {tolerance 0.001}}]
  (letfn [(reducer [m [act comm amt]]
            (assoc-in m [act comm] amt))]

    (let [total (->> (convert-accounts accounts prices "$")
                     (vals)
                     (mapcat vals)
                     (reduce +))]

      (->> (for [[account bal] accounts
                 [commodity amount] bal
                 :let [converted (convert-amount prices commodity "$" amount)
                       fraction (/ converted total)]
                 :when (> fraction tolerance)]
             [account commodity fraction])
           (reduce reducer {})))))


(defn delta
  "Returns absolute differences between accounts"
  [accounts-1 accounts-2]
  (->> (map-values (partial map-values -) accounts-2)
       (merge-with (partial merge-with +) accounts-1)))


(defmulti subaccounts
  "Returns all accounts that satisfy matcher

  if pattern is a string, returns all accounts that have
  a component that contains it as a case insensitive substring

  if pattern is a regex, returns all accounts that have
  acomponent that satisfy it"
  (fn [accounts pattern]
    (type pattern)))


(defmethod subaccounts java.lang.String
  [accounts pattern]
  (let [ptrn (str/lower-case pattern)]
    (->> (for [[account bal :as entry] accounts
               :when (some #(str/includes? (str/lower-case %) ptrn) account)]
           entry)
         (into {}))))


(defmethod subaccounts java.util.regex.Pattern
  [accounts pattern]
  (->> (for [[account bal :as entry] accounts
               :when (some #(re-matches pattern %) account)]
           entry)
       (into {})))
