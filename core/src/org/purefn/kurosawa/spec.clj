(ns org.purefn.kurosawa.spec
  (:require [clojure.spec.alpha :as spec]
            [org.purefn.kurosawa.transform :as xform])
  (:refer-clojure :exclude (def)))

;; Docs are here
(defonce ^:private registry-ref (atom {}))

(defn register
  "Do not call this directly, use `def`"
  [name doc]
  (swap! registry-ref assoc name doc))

(defmacro
  ^{:doc "Registers a spec with optional given docstring. Also calls out
   clojure's spec registry to register the spec, if name is
   registerable."
    :arglists '([name doc-string? spec])}
  def
  [name & attr]
  (let [doc (when (string? (first attr))
              (first attr))
        spec (if (string? (first attr))
               (second attr)
               (first attr))]
    `(do (when (and (ident? ~name) (namespace ~name))
           (spec/def ~name ~spec))
         (when ~doc
           (register ~name ~doc)))))

(defn doc
  "Returns the docstring for a given spec name."
  [name]
  (get @registry-ref name))

(defn with-doc
  "For given spec name, looks up spec (or uses provided) and, if
   docstring is available for that name, returns spec with the
   docstring as `:doc` metadata."
  ([name]
   (with-doc name (spec/get-spec name)))
  ([name spec]
   (when spec
     (let [spec-doc (doc name)]
       (cond-> spec
         spec-doc (vary-meta assoc :doc spec-doc))))))

(defn get-spec
  "Wraps Clojure spec's lookup function, but the returned spec will have
   the docstring as metadata."
  [name]
  (with-doc name))

(defn registry
   "Returns the registry map, keyed by spec name, with documentation metadata
   on each spec."
  []
  (into {}
        (map (fn [[k spec]]
               [k (with-doc k spec)]))
        (spec/registry)))

(defn namespace-specs
  "Returns a collection of all the specs for a given namespace - in
   other words, the specs that have keyword names and the keyword is
   in the given namespace. Each will have their docstring as
   metadata."
  [ns]
  (let [ns (str ns)]
    (into {}
          (comp (filter (comp #{ns} namespace key))
                (map (fn [[k spec]]
                       [k (with-doc k spec)])))
          (spec/registry))))
