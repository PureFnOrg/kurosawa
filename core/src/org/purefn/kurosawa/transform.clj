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

(defn distinct-by
  "Like distinct, but calls f on each item to determine the distinct
   value, while returning the original un-transformed item. Returns a
   transducer if called without a collection argument."
  ([f]
   (fn [rf]
     (let [seen (volatile! #{})]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [d-val (f input)]
            (if (contains? @seen d-val)
              result
              (do (vswap! seen conj d-val)
                  (rf result input)))))))))
  ([f coll]
   (let [step (fn step [xs seen]
                (lazy-seq
                 ((fn [[x :as xs] seen]
                    (when-let [s (seq xs)]
                      (let [d-val (f x)]
                        (if (contains? seen d-val)
                          (recur (rest s) seen)
                          (cons x (step (rest s) (conj seen d-val)))))))
                  xs seen)))]
     (step coll #{}))))

(defn deep-merge
  "Intelligently merges nested maps."
  [& maps]
  (apply merge-with (fn [& args]
                      (if (every? map? args)
                        (apply deep-merge args)
                        (last args)))
         maps))
