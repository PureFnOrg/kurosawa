(defproject org.purefn/kurosawa "2.1.14"
  :description "A catch-all project that brings in all Kurosawa libs."
  :plugins [[lein-modules "0.3.11"]]
  :packaging "pom"

  :dependencies [[org.purefn/kurosawa.aws _]
                 [org.purefn/kurosawa.core _]
                 [org.purefn/kurosawa.log _]
                 [org.purefn/kurosawa.web _]
                 [org.purefn/kurosawa.nrepl _]
                 [com.taoensso/timbre _]
                 [com.stuartsierra/component _]
                 [org.clojure/test.check _]
                 [com.gfredericks/test.chuck _]])
