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
