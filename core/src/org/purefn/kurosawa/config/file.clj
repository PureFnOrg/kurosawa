(ns org.purefn.kurosawa.config.file
  "Load configuration from the filesystem."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [org.purefn.kurosawa.config.parse :as parse]
            [org.purefn.kurosawa.transform :as xform]
            [taoensso.timbre :as log])
  (:import [java.io File]))

(defn- read-directory
  [dir]
  (when (.exists (File. dir))
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
                     (parse/value v)]))]
      (->> (tree-seq dir? files root)
           (filter (comp not dir?))
           (map pairs)
           (reduce (fn [m [p v]] (assoc-in m p v)) {})))))

(defn- read-directories
  [& dirs]
  (log/info "Reading config from" :dirs dirs)
  (->> (map read-directory dirs)
       (apply xform/deep-merge)))

(defn fetch
  ([]
   (fetch "./"))
  ([root-dir]
   (read-directories (str root-dir "configs/")
                     (str root-dir "secrets/"))))
