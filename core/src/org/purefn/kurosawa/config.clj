(ns org.purefn.kurosawa.config
  "Fetch configuration from the environment.

  Presently, config is stored statefully in an `atom` after initial load."
  (:require [org.purefn.kurosawa.config.env :as env]
            [org.purefn.kurosawa.config.file :as file]
            [org.purefn.kurosawa.log.core :as klog]
            [org.purefn.kurosawa.transform :as xform]
            [taoensso.timbre :as log])
  (:import java.io.FileNotFoundException))

(def ^:private config-map
  (atom nil))

(defn set!
  "Set the `config-map` atom to configuration sourced from the envinronment,
  probably from:

  - `org.purefn.kurosawa.config.file/fetch`
  - `org.purefn.kurosawa.config.env/fetch`, or
  - `org.purefn.kurosawa.aws.ssm/fetch`"
  [m]
  (reset! config-map m))

(defn fetch-ssm
  "This does some nasty things to avoid a hard dependency on `aws.ssm` in this project.

  Consider this docstring an apology. `default-config` shouldn't exist as such."
  []
  (try
    ;; avoid a hard dependency on `aws.ssm` from this project.
    (require '[org.purefn.kurosawa.aws.ssm :as ssm])
    ;; not a great place for this, but many components still (sadly) read config
    ;; at *compile time*.  this will spam STDOUT with so much noise otherwise.
    ;; the `:ns-blacklist` will be reset through the typical initialization in
    ;; the `log.core` namespace after this SSM fetch.
    (log/set-config! (update log/*config* :ns-blacklist conj
                             "org.apache.http.*"
                             "com.amazonaws.*"))
    (eval
     '(ssm/fetch
       (or (ssm/prefix-from-env-var)
           "/local/platform")))
    (catch FileNotFoundException ex
      (log/warn "Tried to load org/purefn/kurosawa/aws/ssm but it was"
                "not found in the classpath!"))))


(defn default-config
  "This is our default, precendence based, load config from environment
  mechasnism.  A deep merge is used to create final config map.

  Current precendence is:

  1) Environemt variables
  2) AWS SSM Paramter Store
  3) The filesystem (legacy)

  Ultimately we shouldn't need this, the config stage of application/development
  startup needs to revisited.  But until all of our components' constructors are
  refactored this isn't possible.

  When that day comes we can remove the use of `eval`, the atom, and move to
  a stateless startup sequence, where each component recieves the entire config map and
  parses out the piece it's interested in."
  []
  (xform/deep-merge (file/fetch "/etc/")
                    (fetch-ssm)
                    (env/fetch)))

(defn fetch
  ([]
   (if @config-map
     @config-map
     (reset! config-map (default-config))))
  ([name]
   (get (fetch) name)))
