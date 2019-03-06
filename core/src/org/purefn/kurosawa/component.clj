(ns org.purefn.kurosawa.component)

(defonce ^:private registry-ref (atom {}))

(defn- record-fields
  [type]
  (->> (.getFields type)
       (filter #(not (java.lang.reflect.Modifier/isStatic (.getModifiers %))))
       (filter #(not (clojure.string/starts-with? (.getName %) "__")))
       (map (comp keyword (memfn getName)))))

(defn register
  [type kw]
  (swap! registry-ref assoc kw {:type type
                                :dependencies (record-fields type)}))
