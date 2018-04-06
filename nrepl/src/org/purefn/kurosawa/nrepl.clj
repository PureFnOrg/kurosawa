(ns org.purefn.kurosawa.nrepl
  "Cider nREPL server component."
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.nrepl.server :as repl]
            [cider.nrepl :as cider]
            [com.stuartsierra.component :as component]
            [refactor-nrepl.middleware]
            [taoensso.timbre :as log]))

;;------------------------------------------------------------------------------
;; Component. 
;;------------------------------------------------------------------------------

(defrecord CiderReplServer
    [config server]

  component/Lifecycle
  (start [this]
    (if server
      (do
        (log/info "CiderReplServer already started on port" (::port config))
        this)
      (let [port (::port config)
            middleware (->> cider/cider-middleware
                            (cons 'refactor-nrepl.middleware/wrap-refactor)
                            (map resolve))
            _ (log/info "Starting CiderReplServer on port" port)
            srv (repl/start-server :port port
                                   :handler (apply repl/default-handler
                                                   middleware))]
        (assoc this :server srv))))
  (stop [this]
    (if server
      (do
        (log/info "Stopping CiderReplServer on port" (::port config))
        (repl/stop-server server)
        (assoc this server nil))
      (do
        (log/info "CiderReplServer not running")
        this))))


;;------------------------------------------------------------------------------
;; Configuration. 
;;------------------------------------------------------------------------------

(defn default-config
   "As much of the default configuration as can be determined from the current
   runtime environment.

   - `name` The root of the ConfigMap and Secrets directory.  Defaults to 
   `repl` if not provided."
  ([name] {::port 7888})
  ([] (default-config "repl")))


;;------------------------------------------------------------------------------
;; Creation.
;;------------------------------------------------------------------------------
     
(defn cider-repl-server
  "Creates a CiderReplServer component from config."
  [config]
  (->CiderReplServer config nil))


;;------------------------------------------------------------------------------
;; Specs. 
;;------------------------------------------------------------------------------

(s/def ::port pos-int?)
(s/def ::config (s/keys :opt [::port]))

(s/fdef cider-repl-server
        :args (s/cat :config ::config)
        :ret (partial instance? CiderReplServer))
