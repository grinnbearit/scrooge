(ns scrooge.return)


(defn- aggregate-daily-postings
  "Returns the daily posting amounts, price for every account, commodity, unit"
  [ledger]
  (letfn [(reducer [acc {:keys [account commodity amount unit price]}]
            (update-in acc [account commodity unit] (fnil conj []) {:amount amount
                                                                    :price price}))

          (aggregate-postings [daily-transactions]
            {:date (:date (first daily-transactions))
             :postings (->> (mapcat :postings daily-transactions)
                            (reduce reducer {}))})]

    (->> (partition-by :date ledger)
         (map aggregate-postings))))


(defn- combine-daily-postings
  "Reweights daily postings (account, commodity, unit) into a single price with a weighted sum"
  [daily-postings]
  (letfn [(sum [values]
            {:amount (apply + (map :amount values))
             :price nil})

          (weigh [values]
            (let [total (apply + (map :amount values))
                  cost (->> (map :price values)
                            (map * (map :amount values))
                            (apply +))]
              {:amount total :price (/ cost total)}))

          (combine-postings [{:keys [date postings]}]
            (->> (for [[account commodities] postings
                       [commodity units] commodities
                       [unit values] units]
                   (if (nil? unit)
                     [[account commodity nil] (sum values)]
                     [[account commodity unit] (weigh values)]))
                 (reduce #(assoc-in %1 (%2 0) (%2 1)) {})
                 (assoc {:date date} :balance)))]

    (map combine-postings daily-postings)))


(defn daily-balance
  "Given a ledger, returns daily balance deltas for each account, commodity and unit.
  If multiple prices exist, combines them using the weighted average"
  [ledger]
  (combine-daily-postings (aggregate-daily-postings ledger)))
