(defproject scrooge "2.0.0"
  :description "ledger-cli enhanced reporting"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [metasoarous/oz "1.6.0-alpha2"]]
  :profiles {:dev {:dependencies [[midje "1.9.8"]]}})
