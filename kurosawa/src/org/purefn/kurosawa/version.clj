(ns org.purefn.kurosawa.version
  (:gen-class))

(defn -main
  []
  (println (System/getProperty "kurosawa.version")))
