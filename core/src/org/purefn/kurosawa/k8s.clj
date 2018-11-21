(ns org.purefn.kurosawa.k8s
  "Kubernetes configuration and deployment helpers."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
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

(defn mount-directory
  "Provides mount directory for configs/secrets

   Configs were originally in /etc/configs, but for flexibility
   we moved configs to the project parent level, e.g. for service
   `get-shorty`, this would be /opt/configs, since /opt/get-shorty
   would be our project directory.

   By supporting both setups, we avoid needing to updated all deploy
   projects at the same time. Function name could seem odd, but its
   relates to how kubernetes required volume mounts."
  [type]
  (let [parent-path  (.getParent (io/file (str/trim (:out (shell/sh "pwd")))))
        desired-dir  (str parent-path "/" type "/")
        fallback-dir (str "/etc/" type "/")]
  (if (.isDirectory (io/file desired-dir))
    desired-dir
    fallback-dir)))

(defn config-map
  "Read the Kubernetes ConfigMap from container local disk.

   - `name` The base name of the configuration (if any).

   Returns a nested map of configuration parameters."
  ([name]
   (read-configs (str (mount-directory "configs") name)))
  ([]
   (config-map "")))

(defn secrets
  "Read the Kubernetes Secrets from container local disk.

   - `name` The base name of the secrets (if any).

   Returns a nested map of plain text secrets."
  ([name]
   (read-configs (str (mount-directory "secrets") name)))
  ([]
   (secrets "")))


;;------------------------------------------------------------------------------
;; Specs.
;;------------------------------------------------------------------------------
