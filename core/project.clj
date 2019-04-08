(defproject org.purefn/kurosawa.core "2.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The root Kurosawa library."
  :dependencies [[com.stuartsierra/component _]
                 [org.clojure/test.check _]
                 [org.purefn/kurosawa.log _]
                 [org.purefn/kurosawa.aws _]
                 [com.gfredericks/test.chuck _]
                 [com.taoensso/timbre _]])
