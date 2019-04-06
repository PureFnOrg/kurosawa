(ns org.purefn.kurosawa.config
  "Fetch configuration from the environment.

  Support for fetching configuration from different sources is provided by
  `fetchers`, a vector of functions, each taking a single string argument
  and returning a map."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :refer [instrument]]
            [org.purefn.kurosawa.config.file :as file]
            [org.purefn.kurosawa.config.env :as env]
            [org.purefn.kurosawa.aws.ssm :as ssm]
            [taoensso.timbre :as log]))

(def ^:private fetchers
  (atom [ssm/fetch-config
         env/fetch-config
         (partial file/fetch-config "/etc/")]))

(defn set-config-fetchers!
  [fns]
  (reset! fetchers fns))

(defn fetch-config
  "Attempts to fetch configuration from the sources defined in `fetchers.`

  The default implementation fetches from:
  1) AWS SSM Parameter Store
  2) Environment variables
  3) The filesyetem"
  [name]
  (-> (keep #(% name) @fetchers)
      (first)))

(s/def ::fetcher
  (s/fspec :args (s/cat :s string?)
           :ret (s/nilable map?)))

(s/fdef set-config-fetchers!
  :args (s/cat :fns (s/and vector? (s/coll-of ::fetcher))))

(instrument `set-config-fetchers!)
