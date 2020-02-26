(ns scrooge.report-test
  (:require [midje.sweet :refer :all]
            [scrooge.report :refer :all]
            [scrooge.core :as sc]
            [scrooge.assets :as sa]))


(facts
 "net worth"

 (net-worth "ledger")
 => {"$" -1.0 "BTC" 1.0}

 (provided
  (sc/balance "ledger")
  => "accounts"

  (sc/aggregate "accounts" 0)
  => {["Assets"] {"$" 0.0 "BTC" 2.0}
      ["Liabilities"] {"$" -1.0 "BTC" -1.0}})


 (net-worth "ledger" "prices" "$")
 => {"$" 49.0}

 (provided
  (sc/balance "ledger")
  => "accounts"

  (sc/convert-accounts "accounts" "prices" "$")
  => "converted"

  (sc/aggregate "converted" 0)
  => {["Assets"] {"$" 100.0}
      ["Liabilities"] {"$" -51.0}}))


(facts
 "portfolio"

 (portfolio "ledger" "prices")
 => {"BTC" 0.50 "$" 0.50}

 (provided
  (net-worth "ledger")
  => {"BTC" 10.0 "$" 100.0}

  (sc/fractional {:net-worth {"BTC" 10.0 "$" 100.0}} "prices" :tolerance 0.001)
  => {:net-worth {"BTC" 0.50 "$" 0.50}})

 ;; with include
 (portfolio "ledger" "prices" :include #{"BTC"})
 => {"BTC" 1.00}

 (provided
  (net-worth "ledger")
  => {"BTC" 10.0 "$" 100.0}

  (sc/fractional {:net-worth {"BTC" 10.0}} "prices" :tolerance 0.001)
  => {:net-worth {"BTC" 1.0}})


 ;; with exclude
 (portfolio "ledger" "prices" :exclude #{"$"})
 => {"BTC" 1.00}

 (provided
  (net-worth "ledger")
  => {"BTC" 10.0 "$" 100.0}

  (sc/fractional {:net-worth {"BTC" 10.0}} "prices" :tolerance 0.001)
  => {:net-worth {"BTC" 1.0}}))


(facts
 "valuation"

 (valuation "ledger" "prices" "$")
 => {"BTC" {"$" 100.0} "$" {"$" 100.0}}

 (provided
  (net-worth "ledger")
  => {"BTC" 10.0 "$" 100.0}

  (sc/convert-accounts {"BTC" {"BTC" 10.0} "$" {"$" 100.0}} "prices" "$")
  => {"BTC" {"$" 100.0} "$" {"$" 100.0}}))


(facts
 "asset delta"

 (asset-delta {"BTC" 1.0} "prices" "allocation" {"$" 100})
 => {"$" -100.0 "BTC" 1.0}

 (provided
  (sa/rebalance-assets {"BTC" 1.0} "prices" "allocation" {"$" 100})
  => {"$" -100.0 "BTC" 1.0})


 (asset-delta {"BTC" 1.0} "prices" "allocation" {"$" 100} "$")
 => {"$" -100.0 "BTC" 100.0}

 (provided
  (sa/rebalance-assets {"BTC" 1.0} "prices" "allocation" {"$" 100})
  => {"$" -100.0 "BTC" 1.0}

  (sc/convert-amount "prices" "$" "$" -100.0)
  => -100.0

  (sc/convert-amount "prices" "BTC" "$" 1.0)
  => 100.0))
