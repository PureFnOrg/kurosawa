(ns org.purefn.kurosawa.web.instrument
  "Thread-pool instrumentation for Ring-compliant web server. In most cases you may want
   to use only `initialize` and `instrument` and use the output to launch the web server.

   This library does not include prometheus (iapetos and bidi) and preflex as dependencies
   directly, so requiring this namespace will break unless you've done so yourself."
  (:require [iapetos.core :as prometheus]
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
    (-> registry
        (prometheus/set :http/thread-count (.getPoolSize ^ThreadPoolExecutor (t/thread-pool bounded-thread-pool)))
        (prometheus/set :http/waiting-count (t/queue-size bounded-thread-pool)))
    (when-some [dqm (:duration-queue-ms *context*)]
      (prometheus/set registry :http/wait-time-ms dqm))
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
                instrumented-thread-pool]} (make-server-thread-pool thread-count queue-size)
        app-handler (-> handler
                        (wrap-queue-latency-middleware registry bounded-thread-pool))]
    {:handler app-handler
     :thread-pool instrumented-thread-pool}))

;;*************************************
;; Extension and drop-in replacement for org.purefn.kurosawa.web.app/App
;; for consumption by the org.purefn.kurosawa.web.server namespace
(defrecord InstrumentedApp
  [registry config handler]
  component/Lifecycle
  (start [this] (let [registry (:registry registry) ; get Iapetos registry
                      {:keys [^long server/worker-threads
                              ^long server/queue-capacity
                              ^ExecutorService server/worker-pool]
                       :or {worker-threads server/default-worker-threads
                            queue-capacity server/default-queue-capacity}
                       } config]
                  (log/info "Starting InstrumentedApp")
                  (if (some? worker-pool)
                    this
                    (as-> (instrument registry handler worker-threads queue-capacity) $
                      (update $ :handler vary-meta assoc :orig-handler handler) ; preserve original
                      (merge this $)))))
  (stop [this] (let [{:keys [^ExecutorService server/worker-pool]} config]
                 (if (nil? worker-pool)
                   this
                   (do
                     (.shutdownNow worker-pool) ; accept no new tasks, kill waiting tasks
                     (-> this
                         (assoc-in [:config :server/worker-pool] nil)
                         (update :handler #(-> % meta :orig-handler))))))))

(defn make-instrumented-app
  [config]
  (->InstrumentedApp nil config nil))
