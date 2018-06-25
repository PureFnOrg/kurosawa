(defproject org.purefn/kurosawa.nrepl "2.0.4-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The Kurosawa nREPL library."
  :dependencies [[com.stuartsierra/component _]
                 [com.taoensso/timbre _]
                 [cider/cider-nrepl "0.16.0"]
                 [org.clojure/tools.nrepl "0.2.13"]])
