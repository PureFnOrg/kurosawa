(defproject org.purefn/kurosawa.log "2.1.27"
  :plugins [[lein-modules "0.3.11"]]
  :description "A Kurosawa library for logging."
  :dependencies [[com.taoensso/timbre "4.10.0"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [com.stuartsierra/component "0.3.2"]
                                  [com.stuartsierra/component.repl "0.2.0"]]}})
