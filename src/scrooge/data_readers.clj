(ns scrooge.data-readers
  (:require [clj-time.coerce :as tc]))
;;; https://lambdaisland.com/blog/2017-07-26-dates-in-clojure-making-sense-of-the-mess


;; Configure the printer
(defmethod print-method org.joda.time.LocalDate
  [dt out]
  (.write out (str "#joda/local \"" (.toString dt) "\"") ))


(defmethod print-dup org.joda.time.LocalDate
  [dt out]
  (.write out (str "#joda/local \"" (.toString dt) "\"") ))


;; Utility function for the reader
(defn ->joda-local [time-str]
  (tc/to-local-date time-str))
