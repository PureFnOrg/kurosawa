(defproject org.purefn/kurosawa.log "2.1.5-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "A Kurosawa library for logging."
  :dependencies [[com.taoensso/timbre _]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace _]
                                  [com.stuartsierra/component _]
                                  [com.stuartsierra/component.repl _]]}})
