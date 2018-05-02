(defproject org.purefn/kurosawa.log "0.2.0-SNAPSHOT"
  :description "A Kurosawa library for logging."
  :dependencies [[com.taoensso/timbre _]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace _]
                                  [com.stuartsierra/component _]
                                  [com.stuartsierra/component.repl _]]}})
