(ns org.purefn.kurosawa.config
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
  [name]
  (-> (keep #(% name) @fetchers)
      (first)))

(s/def ::fetcher
  (s/fspec :args (s/cat :s string?)
           :ret (s/nilable map?)))

(s/fdef set-config-fetcher!
  :args (s/cat :fns (s/and vector? (s/coll-of ::fetcher))))

(instrument `set-config-fetchers)
