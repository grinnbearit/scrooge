(ns scrooge.core)


(defn convert-amount
  "Given a dollar-map, converts any 2 commodities"
  [dollar-map from to amount]
  (/ (* amount (dollar-map from))
     (dollar-map to)))


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


(defn net-worth
  "Given a set of account balances, sums all
  accounts with Asset and Liability prefixes"
  [accounts]
  (->> (for [[[root] bal] accounts
             :when (#{"Assets" "Liabilities"} root)]
         {[root] bal})
       (reduce (partial merge-with (partial merge-with +)))))
