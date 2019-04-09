(ns org.purefn.kurosawa.aws.ssm
  "Fetch config from the SSM parameter store."
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :refer [instrument]]
            [taoensso.timbre :as log])
  (:import
   [com.amazonaws.services.simplesystemsmanagement
    AWSSimpleSystemsManagementClientBuilder]
   [com.amazonaws.services.simplesystemsmanagement.model
    GetParametersByPathRequest Parameter]
   [java.net SocketTimeoutException]))

(defn fetch-parameters
  "Fetches parameters from SSM recursively under the given `path`. Returns a map
  of SSM keys to values."
  ([path]
   (fetch-parameters path :noop))
  ([path token]
   (when token
     (let [req (doto (GetParametersByPathRequest.)
                 (.setPath path)
                 (.setRecursive true)
                 (.setWithDecryption true))
           _ (when (not= token :noop)
               (.setNextToken req token))
           resp (-> (AWSSimpleSystemsManagementClientBuilder/defaultClient)
                    (.getParametersByPath req))]
       (merge 
        (->> (.getParameters resp)
             (map (fn [^Parameter p] [(.getName p) (.getValue p)]))
             (into {}))
        (fetch-parameters path (.getNextToken resp)))))))

(defn prefix-from-security-group
  []
  (try 
    (http/get "http://169.254.169.254/latest/meta-data/security-groups/"
              {:socket-timeout 500
               :conn-timeout 500})
    ;; The rest of this is a stub, for now.
    (catch SocketTimeoutException ex
      (log/info "Timed out fetching EC2 metadata, I'm not running on AWS hardware!"))))

(defn remove-trailing
  [s c]
  (if-let [i (str/index-of s c (-> s count dec))]
    (.substring s 0 i)
    s))

(defn prefix-from-env-var
  []
  (some-> (System/getenv "AWS_SSM_PREFIX")
          (remove-trailing "/")))

(def ^:private parse
  (some-fn #(try (Integer. %)
                 (catch Exception ex))
           #(try (Long. %)
                 (catch Exception ex))
           #(try (Double. %)
                 (catch Exception ex))
           identity))

(defn fetch
  [prefix]
  (log/info "Loading config from" :prefix prefix)
  (->> (try (fetch-parameters prefix)
            (catch Exception ex
              (log/warn "Unable to fetch from SSM Parameter Store!"
                        :prefix prefix
                        :message (.getMessage ex))))
       (map (juxt (comp rest #(str/split % #"/") #(str/replace % prefix "") key)
                  val))
       (group-by (comp first first))
       (map (fn [[k kvs]]
              [k (->> (map (juxt (comp second first)
                                 (comp parse second))
                           kvs)
                      (into {}))]))
       (into {})))
