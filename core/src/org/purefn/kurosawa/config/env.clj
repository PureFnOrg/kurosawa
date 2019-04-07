(ns org.purefn.kurosawa.config.env
  "Load configuration from environment variables."
  (:require [clojure.string :as str]
            [org.purefn.kurosawa.config.parse :as parse]))

(defn- munge-name
  [s]
  (str/join
   "-"
   (-> (str/lower-case s)
       (str/split #"_")
       (rest))))

(defn- env-variables*
  []
  (->> (System/getenv)
       (map (juxt (comp str/lower-case key)
                  (comp munge-name key)
                  val))
       (group-by (comp first #(str/split % #"_") first))
       (into {})))

(def env-variables (memoize env-variables*))

(defn fetch-config
  [name]
  (some->> (get (env-variables) name)
           (map (juxt second (comp parse/value last)))
           (into {})))
