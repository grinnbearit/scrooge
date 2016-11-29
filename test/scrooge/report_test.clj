(ns scrooge.report-test
  (:require [midje.sweet :refer :all]
            [scrooge.report :refer :all]
            [scrooge.core :as sc]))


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
