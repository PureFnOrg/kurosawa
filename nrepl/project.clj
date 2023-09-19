(defproject org.purefn/kurosawa.nrepl "2.1.27-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The Kurosawa nREPL library."
  :dependencies [[com.stuartsierra/component "0.3.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [nrepl "0.6.0"]]

  :profiles {:provided
             {:dependencies [[cider/cider-nrepl "0.22.4"]]}})
