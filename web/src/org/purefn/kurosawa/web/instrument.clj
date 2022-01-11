(ns org.purefn.kurosawa.web.instrument
  "Thread-pool instrumentation for Ring-compliant web server. In most cases you may want
   to use only `initialize` and `instrument` and use the output to launch the web server.

   This library does not include prometheus (iapetos and bidi) and preflex as dependencies
   directly, so requiring this namespace will break unless you've done so yourself."
  (:require [clojure.set :as set]
            [iapetos.core :as prometheus]
            [preflex.instrument :as i]
            [preflex.resilient :as r]
            [preflex.type :as t]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [org.purefn.kurosawa.web.server :as server])
  (:import [java.util.concurrent ExecutorService ThreadPoolExecutor]))

;; --- prometheus collectors/registry ---

(def thread-count-collector (prometheus/gauge :http/thread-count
                                              {:description "active thread count for HTTP requests"
                                               :labels      []}))

(def waiting-count-collector (prometheus/gauge :http/waiting-count
                                               {:description "thread-pool queue size for HTTP requests"
                                                :labels      []}))

(def wait-time-ms-collector (prometheus/gauge :http/wait-time-ms
                                              {:description "wait-time (millis) for HTTP requests"
                                               :labels      []}))

(defn initialize
  "Initialize all collectors for thread-pool instrumentation, including
   - http_thread_count
   - http_waiting_count
   - http_wait_time_ms"
  [registry]
  (-> registry
      (prometheus/register
       thread-count-collector
       waiting-count-collector
       wait-time-ms-collector)))

;; --- thread-pool instrumentation ---

(def ^:dynamic *context* {})

(defn wrap-queue-latency-middleware
  [f registry bounded-thread-pool]
  (fn [request]
    (try
      (-> registry
          (prometheus/set :http/thread-count (.getPoolSize ^ThreadPoolExecutor (t/thread-pool bounded-thread-pool)))
          (prometheus/set :http/waiting-count (t/queue-size bounded-thread-pool)))
      (when-some [dqm (:duration-queue-ms *context*)]
        (prometheus/set registry :http/wait-time-ms dqm))
      (catch Exception ex
        (log/error ex "Error logging thread-pool metrics")))
    (f request)))

(defn make-server-thread-pool [thread-count queue-size]
  (let [bounded-pool (r/make-bounded-thread-pool thread-count queue-size)
        task-invoker (fn [g context] (binding [*context* context]
                                       (g)))
        instrumented (i/instrument-thread-pool
                      bounded-pool
                      (assoc i/shared-context-thread-pool-task-wrappers-millis
                             :callable-decorator (i/make-shared-context-callable-decorator task-invoker)
                             :runnable-decorator (i/make-shared-context-runnable-decorator task-invoker)))]
    {:bounded-thread-pool      bounded-pool
     :instrumented-thread-pool instrumented}))

(defn instrument
  "Given Prometheus registry, Ring handler fn and thread-pool parameters,
   construct a bounded thread-pool and return a map containing following
   instrumented objects:

   | Key          | Value description |
   |--------------|-------------------|
   | :handler     | Ring handler that log Prometheus metrics for the thread-pool  |
   | :thread-pool | Thread-pool that captures wait-time in the queue for requests |
   
   You should use the returned thread-pool as the worker-pool in your web server."
  [registry handler thread-count queue-size]
  (let [{:keys [bounded-thread-pool
                instrumented-thread-pool]
         :as tmp} (make-server-thread-pool thread-count queue-size)
        _ (log/info "instrument/" :tmp tmp)
        app-handler (-> handler
                        (wrap-queue-latency-middleware registry bounded-thread-pool))]
    {:handler app-handler
     :bounded-thread-pool bounded-thread-pool
     :thread-pool instrumented-thread-pool}))

;;*************************************
;; Extension and drop-in replacement for org.purefn.kurosawa.web.app/App
;; for consumption by the org.purefn.kurosawa.web.server namespace
(defrecord InstrumentedApp
           [prometheus config handler]
  component/Lifecycle
  (start [this] (if (::server/worker-pool this)
                  this
                  (let [registry (:registry prometheus) ; get Iapetos registry
                        {:keys [^long server/worker-threads
                                ^long server/queue-capacity]
                         :or {worker-threads server/default-worker-threads
                              queue-capacity server/default-queue-capacity}} config]
                    (log/info "Starting InstrumentedApp" :config config :worker-threads worker-threads)
                    (as-> (instrument registry handler worker-threads queue-capacity) $
                      (assoc $ :orig-handler handler)  ; preserve original
                      (set/rename-keys $ {:thread-pool ::server/worker-pool})
                      (merge this $)))))
  (stop [this] (if-some [worker-pool (::server/worker-pool this)]
                 (let [{:keys [orig-handler]} this]
                   (log/info "Stopping InstrumentedApp")
                   (when (nil? orig-handler)
                     (log/warn "Cannot find orig-handler when stopping InstrumentedApp"))
                   (.shutdownNow worker-pool) ; accept no new tasks, kill waiting tasks
                   (-> this
                       (dissoc :orig-handler ::server/worker-pool)
                       (assoc :handler (or orig-handler this))))
                 this)))

(defn make-instrumented-app
  [config handler]
  (->InstrumentedApp nil config handler))
