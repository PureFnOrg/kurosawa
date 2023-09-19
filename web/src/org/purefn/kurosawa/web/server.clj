(ns org.purefn.kurosawa.web.server
  "Immutant web server component."
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :as httpkit-server]
            [ring.adapter.jetty :as jetty]
            [immutant.web :as web]
            [org.purefn.kurosawa.log.api :as log-api]
            [org.purefn.kurosawa.log.core :as klog]
            [org.purefn.kurosawa.log.protocol :as log-proto]
            [org.purefn.kurosawa.web.app :as app]
            [taoensso.timbre :as log])
  (:import org.purefn.kurosawa.web.app.App))

;;------------------------------------------------------------------------------
;; Component.
;;------------------------------------------------------------------------------

(defrecord ImmutantServer
    [config app server]

  component/Lifecycle
  (start [this]
    (if server
      (do
        (log/info "Immutant web server already started")
        this)
      (let [_ (log/info "Starting Immutant web server with" config)
            app-handle (app/app-handler app)
            serv-handle (web/run app-handle
                          (set/rename-keys config
                           {::io-threads :io-threads
                            ::worker-threads :worker-threads
                            ::host :host
                            ::port :port}))]
        (assoc this :server serv-handle))))

  (stop [this]
    (if server
      (do
        (log/info "Stopping Immutant web server on port" (::port config))
        (web/stop server)
        (assoc this :server nil))
      (do
        (log/info "Immutant web server not running")
        this)))


  ;;----------------------------------------------------------------------------
  log-proto/Logging
  (log-namespaces [_]
    ["org.projectodd.wunderboss.*" "org.xnio.*" "org.xnio" "org.jboss.*"])

  (log-configure [this dir]
    (klog/add-component-appender :immutant (log-api/log-namespaces this)
                                 (str dir "/immutant.log"))))


(def default-worker-threads
  "Default worker thread-count for the web server, which is the same as in Immutant,
   that is (* 8 IO-threads) where IO-threads is available processor count."
  (* 8 (.availableProcessors (Runtime/getRuntime))))

(def default-queue-capacity
  "Double-buffering based bounded queue length for requests."
  (* 2 default-worker-threads))


(defrecord HttpkitServer
  [config app server]

  component/Lifecycle
  (start [this]
    (if server
      (do
        (log/info "HTTP-Kit web server already started")
        this)
      (let [_ (log/info "Starting HTTP-Kit web server with" config)
            app-handle (app/app-handler app)
            config (merge {::worker-threads default-worker-threads
                           ::queue-capacity default-queue-capacity
                           :legacy-return-value? false  ; return HttpServer object
                           :error-logger (fn [msg ex] (log/error ex msg))
                           :warn-logger  (fn [msg ex] (log/warn ex msg))
                           :event-logger (fn [event-name]
                                           (when-not (and (some? event-name)
                                                       (string/starts-with? event-name
                                                         ;; do not log 2xx status responses
                                                         "httpkit.server.status.processed.2"))
                                             (log/info event-name)))}
                          (select-keys app [::worker-pool])  ; instrumented app may have instrumented thread-pool
                     config)
            serv-handle (->> {::worker-threads :thread
                              ::queue-capacity :queue-size
                              ::worker-pool :worker-pool
                              ::host :ip
                              ::port :port}
                             (set/rename-keys config)
                             (httpkit-server/run-server app-handle))]
        (assoc this :server serv-handle))))

  (stop [this]
    (if server
      (do
        (log/info "Stopping HTTP-Kit web server on port" (::port config))
        (httpkit-server/server-stop! server)
        (assoc this :server nil))
      (do
        (log/info "HTTP-Kit web server not running")
        this)))


  ;;----------------------------------------------------------------------------
  log-proto/Logging
  (log-namespaces [_]
    [])  ; all logs for HTTP-Kit are in org.purefn.kurosawa.web.server namespace

  (log-configure [this dir]
    (klog/add-component-appender :httpkit (log-api/log-namespaces this)
                                 (str dir "/httpkit.log"))))

(defrecord JettyServer
    [config app server]

  component/Lifecycle
  (start [this]
    (if server
      (do
        (log/info "Jetty web server already started")
        this)
      (let [_ (log/info "Starting Jetty web server with" config)
            {::keys [host port worker-threads]} config
            app-handle (app/app-handler app)
            serv-handle (jetty/run-jetty app-handle
                                         {:join? false
                                          :host host
                                          :port port
                                          :max-threads worker-threads})]
        (assoc this :server serv-handle))))

  (stop [this]
    (if server
      (do
        (log/info "Stopping Jetty web server on port" (::port config))
        (.stop server)
        (.join server)
        (assoc this :server nil))
      (do
        (log/info "Jetty web server not running")
        this)))


  ;;----------------------------------------------------------------------------
  log-proto/Logging
  (log-namespaces [_]
    ["org.eclipse.jetty.*"])

  (log-configure [this dir]
    (klog/add-component-appender :jetty (log-api/log-namespaces this)
                                 (str dir "/jetty.log"))))

;;------------------------------------------------------------------------------
;; Configuration
;;------------------------------------------------------------------------------

(defn default-config
   "As much of the default configuration as can be determined from the current
   runtime environment.

   - `name` The root of the ConfigMap and Secrets directory.  Defaults to
   `server` if not provided."
  ([name]
   {::host "0.0.0.0"
    ::port 8080})
  ([] (default-config "server")))


;;------------------------------------------------------------------------------
;; Creation.
;;------------------------------------------------------------------------------

(defn immutant-server
  "Creates Immutant web server component from optional config and
   optional app.

   - `config`  Web server configuration. `::host` and `::port` are
               required.
   - `app`     If given, should be a Kurosawa app instance."
  ([]
   (immutant-server (default-config) nil))
  ([config]
   (immutant-server config nil))
  ([config app]
   (->ImmutantServer config app nil)))


(defn httpkit-server
  "Creates HTTP-Kit web server component from optional config and
   optional app.

   - `config`  Web server configuration. `::host` and `::port` are
               required.
   - `app`     If given, should be a Kurosawa app instance."
  ([]
   (httpkit-server (default-config) nil))
  ([config]
   (httpkit-server config nil))
  ([config app]
   (->HttpkitServer config app nil)))

(defn jetty-server
  "Creates Jetty web server component from optional config and
   optional app.

   - `config`  Web server configuration. `::host` and `::port` are
               required.
   - `app`     If given, should be a Kurosawa app instance."
  ([]
   (jetty-server (default-config) nil))
  ([config]
   (jetty-server config nil))
  ([config app]
   (->JettyServer config app nil)))


;;------------------------------------------------------------------------------
;; Specs.
;;------------------------------------------------------------------------------

(s/def ::host string?)
(s/def ::port pos-int?)
(s/def ::io-threads pos-int?)
(s/def ::worker-threads pos-int?)
(s/def ::config (s/keys :req [::host ::port]
                        :opt [::io-threads ::worker-threads]))

(s/def ::app (partial instance? App))

(s/fdef immutant-server
        :args (s/or :empty empty?
                    :one-arg (s/cat :config ::config)
                    :two-arg (s/cat :config ::config :app ::app))
        :ret (partial instance? ImmutantServer))

(s/fdef httpkit-server
  :args (s/or :empty empty?
              :one-arg (s/cat :config ::config)
              :two-arg (s/cat :config ::config :app ::app))
  :ret (partial instance? HttpkitServer))

(s/fdef jetty-server
  :args (s/or :empty empty?
              :one-arg (s/cat :config ::config)
              :two-arg (s/cat :config ::config :app ::app))
  :ret (partial instance? JettyServer))
