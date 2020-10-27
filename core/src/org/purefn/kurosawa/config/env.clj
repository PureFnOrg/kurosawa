(ns org.purefn.kurosawa.config.env
  "Load configuration from environment variables."
  (:require [clojure.string :as str]
            [org.purefn.kurosawa.config.parse :as parse]
            [taoensso.timbre :as log]))

(defn- munge-name
  [s]
  (str/join
   "-"
   (-> (str/lower-case s)
       (str/split #"_")
       (rest))))

(defn- env-variables
  []
  (->> (System/getenv)
       (map (juxt (comp str/lower-case key)
                  (comp munge-name key)
                  val))
       (group-by (comp first #(str/split % #"_") first))
       (into {})))

(defn fetch
  "Fetches a map from environment variables according to the convention:

  * MYSQL_USER=root
  * MYSQL_PASSWORD=secure
  * MYSQL_NUM_THREADS=10

  > (fetch)
  {mysql
   {user root
    password secure
    num-threads 10}}"
  []
  (log/info "Reading config from environment variables")
  (->> (System/getenv)
       (map (juxt (comp str/lower-case key)
                  (comp munge-name key)
                  val))
       (group-by (comp first #(str/split % #"_") first))
       (map (fn [[k kvs]]
              [k (->> (map (juxt second
                                 (comp parse/value last))
                           kvs)
                      (into {}))]))
       (into {})))

(defn- candidate-keys
  "Return possible config paths for an environment variable.

  MYSQL_NUM_THREADS -> ([mysql num-threads] [mysql-num threads])"
  [k]
  (let [parts (-> (str/lower-case k)
                  (str/split #"_"))]
    (->> (count parts)
         (range)
         (map (juxt range #(range % (count parts))))
         (filterv (comp seq first))
         (map (fn [[kns vns]]
                [(str/join "-" (map (partial get parts) kns))
                 (str/join "-" (map (partial get parts) vns))])))))

(defn merge-overrides
  "Fetches config from the environment only udpating paths already present in `conf`."
  [conf]
  (reduce-kv
   (fn [m k v]
     (reduce (fn [m [topk _ :as path]]
               (if (get m topk)
                 (assoc-in m path v)
                 m))
             m
             (candidate-keys k)))
   conf
   (into {} (System/getenv))))
