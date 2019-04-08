(ns org.purefn.kurosawa.config
  "Fetch configuration from the environment.

  Presently, config is stored statefully in an `atom` after initial load."
  (:refer-clojure :exclude [set])
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :refer [instrument]]
            [org.purefn.kurosawa.config.file :as file]
            [org.purefn.kurosawa.config.env :as env]
            [org.purefn.kurosawa.aws.ssm :as ssm]
            [taoensso.timbre :as log]))

(def ^:private config-map
  (atom nil))

(defn set
  "Set the `config-map` atom to configuration sourced from the envinronment,
  probably from:

  - `org.purefn.kurosawa.config.file/fetch`
  - `org.purefn.kurosawa.config.env/fetch`, or
  - `org.purefn.kurosawa.aws.ssm/fetch`"
  [m]
  (reset! config-map m))

(defn default-config
  "This is our default, precendence based, load config from environment
  mechasnism.  A shallow merge was chosen to make final merged config map easier
  to reason about.  Current precendence is:

  1) Environemt variables
  2) AWS SSM Paramter Store
  3) The filesystem (legacy)

  Ultimately we shouldn't need this, the config stage of application/development
  startup needs to revisited.  But until all of our components' constructors are
  refactored this isn't possible."
  []
  (merge (file/fetch "/etc/")
         (ssm/fetch (or (ssm/prefix-from-env-var)
                        "/local/platform"))
         (env/fetch)))

(defn fetch
  ([]
   (if @config-map
     @config-map
     (reset! config-map (default-config))))
  ([name]
   (get (fetch) name)))
