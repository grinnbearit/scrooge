(ns scrooge.core
  (:require [clj-time.core :as t]))


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


(defn expenses
  "Given a set of account balances and a level,
  returns all Expenses aggregated at level"
  [accounts level]
  (->> (for [[[root :as account] bal] accounts
             :when (= "Expenses" root)
             :let [sub-acc (if (< level (count account))
                             (subvec account 0 (inc level))
                             account)]]
         {sub-acc bal})
       (reduce (partial merge-with (partial merge-with +)))))


(defn net-worth
  "Given a set of account balances, sums all
  accounts with Asset and Liability prefixes"
  [accounts]
  (->> (for [[[root] bal] accounts
             :when (#{"Assets" "Liabilities"} root)]
         {[root] bal})
       (reduce (partial merge-with (partial merge-with +)))))


(defn portfolio
  "Returns fractions of net-worth for all assets"
  [accounts dollar-map & {:keys [tolerance] :or {tolerance 0.001}}]

  (letfn [(reducer [port [acc amt]]
            (assoc-in port acc amt))]

    (let [dollar-accounts (convert-accounts accounts dollar-map "$")
          total (get-in (net-worth dollar-accounts) [["Assets"] "$"])]

      (->> (for [[[root :as account] bal] accounts
                 :when (= "Assets" root)
                 [commodity amount] bal
                 :let [converted (convert-amount dollar-map commodity "$" amount)
                       fraction (/ converted total)]
                 :when (> fraction tolerance)]
             [(conj account commodity) (/ converted total)])
           (reduce reducer {})))))
