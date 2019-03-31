(defproject scrooge "0.1.0-SNAPSHOT"
  :description "operations on a ledger cli file"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-time "0.15.0"]
                 [swissknife "1.1.0"]]
  :jvm-opts ["-Xmx1G"]
  :profiles {:dev {:dependencies [[midje "1.9.6"]]}})
