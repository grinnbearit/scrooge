(defproject scrooge "0.1.0-SNAPSHOT"
  :description "operations on a ledger cli file"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.zip "0.1.2"]
                 [clj-time "0.12.0"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]}})
