(defproject org.purefn/kurosawa "2.0.3-SNAPSHOT"
  :description "A catch-all project that brings in all Kurosawa libs."
  :plugins [[lein-modules "0.3.11"]]
  :packaging "pom"

  :dependencies [[org.purefn/kurosawa.core _]
                 [org.purefn/kurosawa.log _]
                 [org.purefn/kurosawa.web _]
                 [org.purefn/kurosawa.nrepl _]])
