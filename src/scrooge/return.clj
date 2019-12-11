(ns scrooge.return
  (:require [scrooge.core :as sc]))


(defn ledger->daily-postings
  "Converts a ledger into a list of dated account->postings"
  [ledger]
  (letfn [(reducer [acc {:keys [account commodity amount unit price]}]
            (update-in acc [account commodity unit] (fnil conj []) {:amount amount
                                                                    :price price}))

          (collect-postings [daily-transactions]
            {:date (:date (first daily-transactions))
             :postings (->> (mapcat :postings daily-transactions)
                            (reduce reducer {}))})]

    (->> (partition-by :date ledger)
         (map collect-postings))))


(defn aggregate-daily-postings
  "Given a list of daily-postings and a level,
  returns all daily postings aggregated at level"
  [daily-postings level]
  (letfn [(aggregate-postings [{:keys [postings] :as daily-posting}]
            (->> (for [[[root :as account] commodities] postings
                       :let [sub-acc (if (< level (count account))
                                       (subvec account 0 (inc level))
                                       account)]
                       [commodity units] commodities
                       [unit stubs] units]
                   [[sub-acc commodity unit] stubs])
                 (reduce #(update-in %1 (%2 0) (fnil into []) (%2 1)) {})
                 (assoc daily-posting :postings)))]

    (map aggregate-postings daily-postings)))


(defn daily-subaccounts
  "Returns a subset of daily-postings that satisfy at least one `matcher`"
  [daily-postings matchers]
  (for [{:keys [postings] :as daily-posting} daily-postings]
    (assoc daily-posting :postings (sc/subaccounts postings matchers))))


(defn daily-postings->daily-balance
  "Reweights daily postings into a single stub, the price is a weighted sum
  Removes zero balances"
  [daily-postings]
  (letfn [(merge-stubs [stubs]
            (if (:price (first stubs))
              (let [amounts (map :amount stubs)
                    qntys (map #(Math/abs %) amounts)
                    cost (->> (map :price stubs)
                              (map * qntys)
                              (apply +))]
                {:amount (apply + amounts)
                 :price (/ cost (apply + qntys))})
              {:amount (apply + (map :amount stubs)) :price nil}))

          (merge-postings [{:keys [date postings]}]
            (->> (for [[account commodities] postings
                       [commodity units] commodities
                       [unit stubs] units
                       :let [stub (merge-stubs stubs)]
                       :when (not (zero? (:amount stub)))]
                   [[account commodity unit] stub])
                 (reduce #(assoc-in %1 (%2 0) (%2 1)) {})
                 (assoc {:date date} :balance)))]

    (map merge-postings daily-postings)))


(defn daily-postings->daily-net
  "Aggregates all postings to daily net commodites"
  [daily-postings]
  (letfn [(balance->net [{:keys [balance date]}]
            {:date date
             :net (balance [])})]

    (->> (aggregate-daily-postings daily-postings -1)
         (daily-postings->daily-balance)
         (map balance->net))))
