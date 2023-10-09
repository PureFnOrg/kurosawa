(defproject org.purefn/kurosawa.core "2.1.29-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The root Kurosawa library."
  :dependencies [[com.stuartsierra/component "0.3.2"]
                 [org.clojure/test.check "0.9.0"]
                 [org.purefn/kurosawa.log "2.1.29-SNAPSHOT"]
                 [com.gfredericks/test.chuck "0.2.7"]
                 [com.taoensso/timbre "4.10.0"]]
  :profiles
  {:dev {:dependencies [[org.purefn/kurosawa.aws "2.1.29-SNAPSHOT"]]}})
