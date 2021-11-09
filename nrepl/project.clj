(defproject org.purefn/kurosawa.nrepl "2.1.17"
  :plugins [[lein-modules "0.3.11"]]
  :description "The Kurosawa nREPL library."
  :dependencies [[com.stuartsierra/component _]
                 [com.taoensso/timbre _]
                 [nrepl "0.6.0"]]

  :profiles {:provided
             {:dependencies [[cider/cider-nrepl "0.22.4"]]}})
