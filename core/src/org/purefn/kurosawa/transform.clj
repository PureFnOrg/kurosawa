(ns org.purefn.kurosawa.transform)

(defn extract-keys
  "Like rename-keys, except that it will not include keys not included
   in the replacement map or the keep vector in the returned map.
   Takes a map and a map of key to replacement key. Takes an optional
   third argument, which is a convenience vector of keys to keep
   without renaming.

   (extract-keys {:a 1 :b 2 :c 3} {:b ::b} [:c])
     => {::b 2 :c 3}"
  ([map kmap]
   (extract-keys map kmap []))
  ([map kmap keep]
   (let [extracted (reduce (fn [m [old new]]
                             (if (contains? map old)
                               (assoc m new (get map old))
                               m))
                           {}
                           kmap)]
     (merge (select-keys map keep) extracted))))

(defn filter-vals
  "Takes a predicate and a map, and returns a map containing only the
   keys for which (pred val) returns true. Returns a transducer when
   no input map is provided."
  ([pred]
   (filter (comp pred val)))
  ([pred m]
   (into {} (filter (comp pred val)) m)))

(defn map-vals
  "Returns a map consisting of the result of applying f to each value in
   m. Returns a transducer when no input map is provided."
  ([f]
   (map #(clojure.lang.MapEntry. (key %) (f (val %)))))
  ([f m]
   (into {} (map (juxt key (comp f val))) m)))
