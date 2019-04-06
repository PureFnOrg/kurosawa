(ns org.purefn.kurosawa.config.file
  "Load configuration from the filesystem."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [org.purefn.kurosawa.result :refer :all])
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
                     (-> (attempt (fn [^String s] (Integer. s)) v)
                         (recover (fn [_] (Long. v)))
                         (recover (fn [_] (Double. v)))
                         (recover (constantly v))
                         (success))]))]
      (->> (tree-seq dir? files root)
           (filter (comp not dir?))
           (map pairs)
           (reduce (fn [m [p v]] (assoc-in m p v)) {})))))

(defn- read-directories
  [& dirs]
  (->> (map read-directory dirs)
       (reduce merge)))

(defn fetch-config
  ([name]
   (fetch-config "./" name))
  ([root-dir name]
   (read-directories (str root-dir "configs/" name)
                     (str root-dir "secrets/" name))))
