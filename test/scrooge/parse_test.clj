(ns scrooge.parse-test
  (:require [datetime]
            [clj-time.core :refer [local-date]]
            [midje.sweet :refer :all]
            [scrooge.parse :refer :all])
  (:import [java.io BufferedReader StringReader]))


(facts
 (parse-prices
  (BufferedReader.
   (StringReader.
    (str "P 2016/09/17 09:28:53 ₹ $ 0.014900\n"
         "P 2016/09/17 09:28:56 € $ 1.115800"))))
 => [{:date (local-date 2016 9 17)
      :commodity "₹"
      :unit "$"
      :price 0.0149}
     {:date (local-date 2016 9 17)
      :commodity "€"
      :unit "$"
      :price 1.1158}])
