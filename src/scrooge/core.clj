(ns scrooge.core
  (:require [clj-time.core :as t]
            [swissknife.core :refer [map-values]]))


(defn convert-amount
  "Given a dollar-map, converts any 2 commodities"
  [dollar-map from to amount]
  (/ (* amount (dollar-map from))
     (dollar-map to)))


(defn postings
  "Filters ledger entries"
  [ledger & {:keys [from to]}]
  (for [{:keys [date] :as transaction} ledger
        :when (and (or (nil? from)
                       (not (t/before? date from)))
                   (or (nil? to)
                       (t/before? date to)))]
    transaction))


(defn balance
  "Returns the balance amount in every account"
  [ledger]
  (->> (for [transaction ledger
             {:keys [account commodity amount]} (:postings transaction)]
         {account {commodity amount}})
       (reduce (partial merge-with (partial merge-with +)))))


(defn convert-accounts
  "Convert all account balances to the same commodity"
  [accounts dollar-map to]
  (->> (for [[account bal] accounts
             [commodity amount] bal]
         {account {to (convert-amount dollar-map commodity to amount)}})
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


(defn match
  "Given a set of account balances and a pattern
  returns all accounts having a piece that matches the pattern"
  [accounts pattern]
  (->> (for [[account bal] accounts
             :when (some #(= pattern %) account)]
         [account bal])
       (into {})))


(defn fractional
  "Given a set of account balances and a dollar map returns
   fractions of the whole for each account and currency"
  [accounts dollar-map & {:keys [tolerance] :or {tolerance 0.001}}]
  (letfn [(reducer [m [act comm amt]]
            (assoc-in m [act comm] amt))]

    (let [total (->> (convert-accounts accounts dollar-map "$")
                     (vals)
                     (mapcat vals)
                     (reduce +))]

      (->> (for [[account bal] accounts
                 [commodity amount] bal
                 :let [converted (convert-amount dollar-map commodity "$" amount)
                       fraction (/ converted total)]
                 :when (> fraction tolerance)]
             [account commodity fraction])
           (reduce reducer {})))))


(defn delta
  "Returns absolute differences between accounts"
  [accounts-1 accounts-2]
  (->> (map-values (partial map-values -) accounts-2)
       (merge-with (partial merge-with +) accounts-1)))
