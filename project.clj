(defproject org.purefn/kurosawa "2.0.7-SNAPSHOT"
  :description "Parent for all that is Kurosawa"
  :plugins [[lein-modules "0.3.11"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure _]]}

             :dev {:dependencies [[org.clojure/tools.namespace _]
                                  [com.stuartsierra/component.repl _]]
                   :jvm-opts ["-Xmx2g"]
                   :source-paths ["dev"]
                   :codeina {:sources ["src"]
                             :exclude [org.purefn.kurosawa.version]
                             :reader :clojure
                             :target "doc/dist/latest/api"
                             :src-uri "http://github.com/PureFnOrg/kurosawa/blob/master/"
                             :src-uri-prefix "#L"}
                   :plugins [[funcool/codeina "0.4.0"
                              :exclusions [org.clojure/clojure]]
                             [lein-ancient "0.6.10"]]}}

  :aliases {"project-version" ["run" "-m" "org.purefn.kurosawa.version"]}

  :modules  {:subprocess nil
             
             :inherited {:min-lein-version "2.7.1"
                         :aliases      {"all" ^:displace ["do" "clean," "test," "install"]
                                        "-f" ["with-profile" "+fast"]}
                         :license {:name "Apache Software License - v 2.0"
                                   :url "http://www.apache.org/licenses/LICENSE-2.0"}
                         :url "https://github.com/PureFnOrg/kurosawa"
                         :deploy-repositories
                         [["releases" {:url "https://clojars.org/repo/" :creds :gpg}]]}

             :versions {org.clojure/clojure             "1.9.0"
                        com.taoensso/timbre             "4.10.0"
                        com.stuartsierra/component      "0.3.2"
                        com.stuartsierra/component.repl "0.2.0"
                        org.clojure/test.check          "0.9.0"
                        com.gfredericks/test.chuck      "0.2.7"
                        org.clojure/tools.namespace     "0.2.11"
                        org.purefn/kurosawa.log         :version
                        org.purefn/kurosawa.core        :version
                        org.purefn/kurosawa.web         :version
                        org.purefn/kurosawa.nrepl       :version}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["modules" "change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["modules" "deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["modules" "change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
