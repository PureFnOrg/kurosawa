(defproject org.purefn/kurosawa.web "0.2.0-SNAPSHOT"
  :description "The Kurosawa web library."
  :dependencies [[org.clojure/clojure _]
                 [com.stuartsierra/component _]

                 [org.immutant/web "2.1.7"
                  :exclusions [ch.qos.logback/logback-classic]]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [com.fzakaria/slf4j-timbre "0.3.5"]

                 [org.purefn/kurosawa.log :version]])
