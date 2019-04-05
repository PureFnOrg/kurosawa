(ns org.purefn.kurosawa.config
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :refer [instrument]]
            [taoensso.timbre :as log]))

(def fetchers (atom {}))

(defn set-config-fetcher!
  [f]
  (swap! fetchers update :config
         (fn [g]
           (when g
             (log/warn "Replacing config fetch-fn" g))
           f)))

(defn set-secret-fetcher!
  [f]
  (swap! fetchers update :config
         (fn [g]
           (when g
             (log/warn "Replacing config fetch-fn" g))
           f)))

(s/def ::fetcher
  (s/fspec :args (s/cat :s string?)
           :ret (s/nilable map?)))

(s/fdef set-config-fetcher!
  :args (s/cat :f ::fetcher))

(instrument `set-config-fetcher!)
