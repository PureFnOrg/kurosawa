(defproject org.purefn/kurosawa.core "2.1.20"
  :plugins [[lein-modules "0.3.11"]]
  :description "The root Kurosawa library."
  :dependencies [[com.stuartsierra/component _]
                 [org.clojure/test.check _]
                 [org.purefn/kurosawa.log _]
                 [com.gfredericks/test.chuck _]
                 [com.taoensso/timbre _]]
  :profiles
  {:dev {:dependencies [[org.purefn/kurosawa.aws _]]}})
