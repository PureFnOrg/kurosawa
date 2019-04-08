(ns org.purefn.kurosawa.config
  "Fetch configuration from the environment.

  Presently, config is stored statefully in an `atom` after initial load."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :refer [instrument]]
            [org.purefn.kurosawa.config.file :as file]
            [org.purefn.kurosawa.config.env :as env]
            [org.purefn.kurosawa.aws.ssm :as ssm]
            [taoensso.timbre :as log]))

(def ^:private config-map
  (atom nil))

(defn default-config
  "This is our default, precendence based, load config from environment
  mechasnism.  A shallow merge was chosen to make final merged config map easier
  to reason about."
  []
  (merge (file/fetch "etc/")
         (ssm/fetch (or (ssm/prefix-from-security-group)
                        "/local/platform"))
         (env/fetch)))

(defn reset
  "Reset the `config-map` atom to something other than the default."
  [m]
  (reset! config-map m))

(defn fetch
  ([]
   (if @config-map
     @config-map
     (reset! config-map (default-config))))
  ([name]
   (get (fetch) name)))
