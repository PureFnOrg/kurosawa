(defproject org.purefn/kurosawa "1.12.6-SNAPSHOT"
  :description "The root Kurosawa library."
  :url "https://github.com/TheLadders/kurosawa"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.7.1"
  ;; :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [com.stuartsierra/component "0.3.2"]

                 [org.clojure/test.check "0.9.0"]
                 [com.gfredericks/test.chuck "0.2.7"
                  :exclusions [clj-time]]
                 
                 [com.taoensso/timbre "4.10.0"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [com.fzakaria/slf4j-timbre "0.3.5"]
                 
                 [cider/cider-nrepl "0.14.0"]
                 [refactor-nrepl "2.3.0"]
                 
                 [org.immutant/web "2.1.7"
                  :exclusions [ch.qos.logback/logback-classic]]]
  :profiles
  {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]
         :jvm-opts ["-Xmx2g"]
         :source-paths ["dev"]
         :codeina {:sources ["src"]
                   :exclude [org.purefn.kurosawa.version]
                   :reader :clojure
                   :target "doc/dist/latest/api"
                   :src-uri "http://github.com/TheLadders/kurosawa/blob/master/"
                   :src-uri-prefix "#L"}
         :plugins [[funcool/codeina "0.4.0"
                    :exclusions [org.clojure/clojure]]
                   [lein-ancient "0.6.10"]]}}
  :aliases {"project-version" ["run" "-m" "org.purefn.kurosawa.version"]})
