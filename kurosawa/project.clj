(defproject org.purefn/kurosawa "2.1.29-SNAPSHOT"
  :description "A catch-all project that brings in all Kurosawa libs."
  :plugins [[lein-modules "0.3.11"]]
  :packaging "pom"

  :dependencies [[org.purefn/kurosawa.aws "2.1.29-SNAPSHOT"]
                 [org.purefn/kurosawa.core "2.1.29-SNAPSHOT"]
                 [org.purefn/kurosawa.log "2.1.29-SNAPSHOT"]
                 [org.purefn/kurosawa.web "2.1.29-SNAPSHOT"]
                 [org.purefn/kurosawa.nrepl "2.1.29-SNAPSHOT"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.clojure/test.check "0.9.0"]
                 [com.gfredericks/test.chuck "0.2.7"]])
