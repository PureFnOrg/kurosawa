(defproject org.purefn/kurosawa.nrepl "0.1.0-SNAPSHOT"
  :description "The Kurosawa nREPL library."
  :url "https://github.com/TheLadders/kurosawa"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [com.stuartsierra/component "0.3.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [cider/cider-nrepl "0.14.0"]
                 [refactor-nrepl "2.3.1"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [com.stuartsierra/component.repl "0.2.0"]]
                   :source-paths ["dev"]}})
