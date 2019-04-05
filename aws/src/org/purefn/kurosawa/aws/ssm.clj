(ns org.purefn.kurosawa.aws.ssm
  (:require [clj-http.client :as http]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :refer [instrument]]
            [taoensso.timbre :as log])
  (:import
   [com.amazonaws.services.simplesystemsmanagement
    AWSSimpleSystemsManagementClientBuilder]
   [com.amazonaws.services.simplesystemsmanagement.model
    GetParametersByPathRequest Parameter]
   [java.net SocketTimeoutException]))

(defn- fetch-parameters
  "Fetches parameters from SSM recursively under the given `path`. Returns a map
  of SSM keys to values."
  [path]
  (let [req (doto (GetParametersByPathRequest.)
              (.setPath path)
              (.setRecursive true)
              (.setWithDecryption true))
        resp (-> (AWSSimpleSystemsManagementClientBuilder/defaultClient)
                 (.getParametersByPath req))]
    (->> (.getParameters resp)
         (map (fn [^Parameter p] [(.getName p) (.getValue p)]))
         (into {}))))

(def ^:private default-prefix-resolver
  ;; current convention
  (constantly "/local/platform/"))

(defn- security-group-resolver
  []
  (try 
    (http/get "http://169.254.169.254/latest/meta-data/security-groups/"
              {:socket-timeout 500
               :conn-timeout 500})
    ;; The rest of this is a stub, for now.
    (catch SocketTimeoutException ex
      (log/info "Timed out fetching EC2 metadata, I'm not running on AWS hardware!"))))

(def ^:private prefix-resolvers
  (atom [security-group-resolver
         default-prefix-resolver]))

(defn prefix
  []
  (-> (keep (fn [f] (f)) @prefix-resolvers)
      (first)))

(defn set-prefix-resolvers!
  [fns]
  (reset! prefix-resolvers fns))

(s/def ::resolver (s/fspec :ret (s/nilable string?)))

(s/fdef set-prefix-resolvers
  :args (s/cat :fns (s/coll-of ::resolver)))

(instrument `set-prefix-resolvers!)