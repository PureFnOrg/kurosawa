(defproject org.purefn/kurosawa.web "0.1.0-SNAPSHOT"
  :description "The Kurosawa web library."
  :url "https://github.com/PureFnOrg/kurosawa"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [com.stuartsierra/component "0.3.2"]

                 [org.immutant/web "2.1.7"
                  :exclusions [ch.qos.logback/logback-classic]]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [com.fzakaria/slf4j-timbre "0.3.5"]

                 [org.purefn/kurosawa.log "0.1.0-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [com.stuartsierra/component.repl "0.2.0"]]
                   :source-paths ["dev"]}})
