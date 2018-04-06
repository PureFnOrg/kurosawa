(ns org.purefn.kurosawa.log.api
  (:require [clojure.spec.alpha :as s]
            [org.purefn.kurosawa.log.protocol :as proto]))

;;------------------------------------------------------------------------------
;; Logging API 
;;------------------------------------------------------------------------------

(defn log-namespaces
   "A sequence of one or more string patterns matching the namespaces of
    low-level loggers associated with the component."
  [comp]
  (if (satisfies? proto/Logging comp)
    (proto/log-namespaces comp)
    []))

(defn log-configure
  "Returns a function which when applied to the Timbre logging configuration
   will return a new configuration, possibly modified.

   - `dir` The root logging directory."
  [comp dir]
  (if (satisfies? proto/Logging comp)
    (proto/log-configure comp dir)
    identity))


;;------------------------------------------------------------------------------
;; Specs
;;------------------------------------------------------------------------------

(def logger? (partial satisfies? proto/Logging))

(s/fdef log-namespaces
        :args (s/cat :comp logger?)
        :ret (s/coll-of string? :kind vector?))

(s/fdef log-configure
        :args (s/cat :comp logger? :dir string?)
        :ret (s/fspec :args (s/cat :config map?)
                      :ret map?))
