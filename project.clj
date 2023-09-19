(defproject org.purefn/kurosawa "2.1.27-SNAPSHOT"
  :description "Parent for all that is Kurosawa"
  :plugins [[lein-modules "0.3.11"]]

  :profiles {:provided {:dependencies [;; pinning this version here is required for
                                       ;; refactor-nrepl to start-up
                                       [org.clojure/clojure "1.10.0"]]}

             :dev {:dependencies [[org.clojure/tools.namespace "1.1.0"]
                                  [com.stuartsierra/component.repl "0.2.0"]]
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
                         :aliases {"all" ^:displace ["do" "clean," "test," "install"]
                                   "-f" ["with-profile" "+fast"]}
                         :license {:name "Apache Software License - v 2.0"
                                   :url "http://www.apache.org/licenses/LICENSE-2.0"}
                         :url "https://github.com/PureFnOrg/kurosawa"
                         :deploy-repositories
                         [["releases" {:url "https://clojars.org/repo/" :creds :gpg}]
                          ["snapshots" {:url "https://clojars.org/repo/" :creds :gpg}]]}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["modules" "change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["modules" "deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["modules" "change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
