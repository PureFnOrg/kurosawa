(ns org.purefn.kurosawa.component)

(defonce ^:private registry-ref (atom {}))

(defn- record-fields
  [type]
  (->> (.getFields type)
       (filter #(not (java.lang.reflect.Modifier/isStatic (.getModifiers %))))
       (filter #(not (clojure.string/starts-with? (.getName %) "__")))
       (map (comp keyword (memfn getName)))))

(defn register
  [type factory-fn kw & {:keys [config]}]
  (let [config-fn (or config
                      (resolve (symbol (str (namespace-munge *ns*) "/default-config"))))]
    (swap! registry-ref assoc kw {:type type
                                  :factory factory-fn
                                  :config config-fn
                                  :dependencies (record-fields type)})))
