(defproject scrooge "0.1.0-SNAPSHOT"
  :description "operations on a ledger cli file"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.zip "0.1.2"]
                 [clj-time "0.14.2"]
                 [swissknife "1.0.0"]]
  :profiles {:dev {:dependencies [[midje "1.9.0"]]}})
