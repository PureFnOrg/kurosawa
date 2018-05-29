(ns org.purefn.kurosawa.k8s
  "Kubernetes configuration and deployment helpers."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [org.purefn.kurosawa.result :refer :all])
  (:import [java.io File]))

;;------------------------------------------------------------------------------
;; API
;;------------------------------------------------------------------------------

(defn kubernetes?
  "Is the program currently running in a Kubernetes container?"
  []
  (-> (System/getenv)
      (get "KUBERNETES_SERVICE_HOST")
      some?))

(defn- read-configs
  [dir]
  (let [n (-> (str/split dir #"/")
              (count))                 
        ^File root (io/file dir)
        dir? (fn [^File fd] (.isDirectory fd))
        files (fn [^File d]
                (if (some->> d (.getName) (re-find #"\.\..+"))
                  []
                  (.listFiles d)))
        pairs (fn [^File fd]
                (let [p (-> (.getPath fd)
                            (str/split #"/"))
                      v (-> (slurp fd)
                            (str/trim))]
                  [(drop n p)
                   (-> (attempt (fn [^String s] (Integer. s)) v)
                       (recover (fn [_] (Long. v)))
                       (recover (fn [_] (Double. v)))
                       (recover (constantly v))
                       (success))]))]
    (->> (tree-seq dir? files root)
         (filter (comp not dir?))
         (map pairs)
         (reduce (fn [m [p v]] (assoc-in m p v)) {}))))
         

(defn config-map
  "Read the Kubernetes ConfigMap from container local disk.

   - `name` The base name of the configuration (if any).
   
   Returns a nested map of configuration parameters."
  ([name]
   (read-configs (str "/etc/configs/" name)))
  ([]
   (config-map "")))

(defn secrets
  "Read the Kubernetes Secrets from container local disk.

   - `name` The base name of the secrets (if any).
   
   Returns a nested map of plain text secrets."
  ([name]
   (read-configs (str "/etc/secrets/" name)))
  ([]
   (secrets "")))


;;------------------------------------------------------------------------------
;; Specs. 
;;------------------------------------------------------------------------------
