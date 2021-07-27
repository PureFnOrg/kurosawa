(ns org.purefn.kurosawa.web.prometheus
  "Includes a prometheus ring middleware using iapetos and bidi.  Bidi's route
  matching is required to remove route params from the time series.

  This library does not include iapetos and bidi as dependencies directly, so
  requiring this namespace will break unless you've done so yourself."
  (:require [bidi.bidi :as bidi]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [org.purefn.kurosawa.web.app :as app]
            [iapetos.core :as prometheus]
            [iapetos.collector.ring :as ring]
            [iapetos.collector.exceptions :as ex]
            [iapetos.export :as export]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:import [iapetos.registry IapetosRegistry]
           [io.prometheus.client.exporter.common TextFormat]))

(defonce registry
  (-> (prometheus/collector-registry)
      (ring/initialize)))

;;--------------------------------------------------------------------------------
;; Stateful component

(defrecord PrometheusRegistry
    [registration]

  component/Lifecycle
  (start [this]
    (log/info "Starting PrometheusRegistry")
    (assoc
     this
     :registry
     (reduce
      (fn [reg f]
        (let [reg (f reg)]
          (when-not (instance? IapetosRegistry reg)
            (throw (ex-info (str "Registration fn passed to PrometheusRegistry must "
                                 "return an instance of IapetosRegistry"
                                 {:returned reg}))))
          reg))
      (-> (prometheus/collector-registry)
          (ring/initialize))
      registration)))

  (stop [this]
    this))

(s/def ::registration (s/coll-of fn?))

(defn create-registry
  "Creates PrometheusRegistry which stuartsierra.component knows how to start.

  `registration` must be a list of 1-arity functions expecting and returning an
  `IaopetosRegistry`.  Each fn will be called on startup to register Prometheus metrics."
  [registration]
  {:pre [(s/assert* ::registration registration)]}
  (PrometheusRegistry. registration))

;;--------------------------------------------------------------------------------
;; Middleware
;;
;; ## Note
;;
;; This implementation stays purposefully close to the one in
;; 'clj-commons/iapetos' at iaopetos/collector/ring.clj in regard to metric naming
;; and histogram bucket selection, originally reproduced from
;; 'soundcloud/prometheus.clj'. prometheus-clj was published under the Apache
;; License 2.0:
;;
;;    Copyright 2014 SoundCloud, Inc.
;;
;;    Licensed under the Apache License, Version 2.0 (the "License"); you may
;;    not use this file except in compliance with the License. You may obtain
;;    a copy of the License at
;;
;;        http://www.apache.org/licenses/LICENSE-2.0
;;
;;    Unless required by applicable law or agreed to in writing, software
;;    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
;;    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
;;    License for the specific language governing permissions and limitations
;;    under the License.

;; ## Initialization

(defn- make-latency-collector
  [labels buckets]
  (prometheus/histogram
    :http/request-latency-seconds
    {:description "the response latency for HTTP requests."
     :labels (concat [:method :status :statusClass :path] labels)
     :buckets buckets}))

(defn- make-count-collector
  [labels]
  (prometheus/counter
    :http/requests-total
    {:description "the total number of HTTP requests processed."
     :labels (concat [:method :status :statusClass :path] labels)}))

(defn- make-exception-collector
  [labels]
  (ex/exception-counter
    :http/exceptions-total
    {:description "the total number of exceptions encountered during HTTP processing."
     :labels (concat [:method :path] labels)}))

(defn initialize
  "Initialize all collectors for Ring handler instrumentation. This includes:

   - `http_request_latency_seconds`
   - `http_requests_total`
   - `http_exceptions_total`

   Additional `:labels` can be given which need to be supplied using a
   `:label-fn` in [[wrap-instrumentation]] or [[wrap-metrics]].  "
  [registry
   & [{:keys [latency-histogram-buckets labels]
       :or {latency-histogram-buckets [0.001 0.005 0.01 0.02 0.05 0.1 0.2 0.3
                                       0.5 0.75 1 5]}}]]
  (prometheus/register
    registry
    (make-latency-collector labels latency-histogram-buckets)
    (make-count-collector labels)
    (make-exception-collector labels)))

;; ## Response

(defn metrics-response
  "Create a Ring response map describing the given collector registry's contents
   using the text format (version 0.0.4)."
  [registry]
  {:status 200
   :headers {"Content-Type" TextFormat/CONTENT_TYPE_004}
   :body    (export/text-format registry)})

;; ### Latency/Count

(defn- ensure-response-map
  [response]
  (cond (nil? response)          {:status 404}
        (not (map? response))    {:status 200}
        (not (:status response)) (assoc response :status 200)
        :else response))

(defn- status-class
  [{:keys [status]}]
  (str (quot status 100) "XX"))

(defn- status
  [{:keys [status]}]
  (str status))

(defn- labels-for
  ([options request]
   (labels-for options request nil))
  ([{:keys [label-fn path-fn]} {:keys [request-method] :as request} response]
   (merge {:path   (path-fn request)
           :method (-> request-method name string/upper-case)}
          (label-fn request response))))

(defn- record-metrics!
  [{:keys [registry] :as options} delta request response]
  (let [labels           (merge
                           {:status      (status response)
                            :statusClass (status-class response)}
                           (labels-for options request response))
        delta-in-seconds (/ delta 1e9)]
    (-> registry
        (prometheus/inc     :http/requests-total labels)
        (prometheus/observe :http/request-latency-seconds labels delta-in-seconds))))

(defn- exception-counter-for
  [{:keys [registry] :as options} request]
  (->> (labels-for options request)
       (registry :http/exceptions-total)))

(defn- run-instrumented
  [{:keys [handler] :as options} request]
  (ex/with-exceptions (exception-counter-for options request)
    (let [start-time (System/nanoTime)
          response   (handler request)
          delta      (- (System/nanoTime) start-time)]
      (->> (ensure-response-map response)
           (record-metrics! options delta request))
      response)))

;;--------------------------------------------------------------------------------
;; Middleware API

(defn path
  [routes ignore-keys {:keys [uri request-method]}]
  (or (->> (bidi/match-route routes uri :request-method request-method)
           (:route-params)
           (remove (comp (set ignore-keys)
                         first))
           (reduce (fn [path [p v]]
                     (string/replace path v (str p)))
                   uri))
      uri))

(defn wrap-instrumentation
  "Like iapetos' `wrap-instrumentation` but fetches the Prometheus registry from
  the request map at request time, where iapetos captures the references in a closure.

  For more detail see iapetos.collector.ring/wrap-instrumentation:
  https://github.com/clj-commons/iapetos/blob/0fecedaf8454e17e41b05e0e14754a311b9f4ce2/src/iapetos/collector/ring.clj#L143"
  [handler [{:keys [path-fn label-fn]
             :or {path-fn  :uri
                  label-fn (constantly {})}
             :as options}]]
  (fn [request]
    (run-instrumented {:path-fn  path-fn
                       :label-fn label-fn
                       :registry (:registry (app/service request :prometheus))
                       :handler  handler}
                      request)))

(defn wrap-metrics-expose
  "Expose Prometheus metrics at the given constant URI using the text format."
  [handler & [{:keys [path on-request]
               :or {path       "/metrics"
                    on-request identity}}]]
  (fn [{:keys [request-method uri] :as request}]
    (if (= uri path)
      (if (= request-method :get)
        (metrics-response (:registry (app/service request :prometheus)))
        {:status 405})
      (handler request))))

(defn wrap-stateful-metrics
  "Wrap all ring requests with prometheus instrumentation and exposes metrics
  `/metrics`.  Does not require a prometheus registry be passed in.

  - `handler` the ring handler fn
  - `routes` bidi web routes"
  [handler routes {:keys [ignore-keys] :as options}]
  (let [path-fn (partial path routes ignore-keys)]
    (-> handler
        (wrap-instrumentation {:path-fn path-fn})
        (wrap-metrics-expose))))

(defn wrap-metrics
  "Wrap all ring requests with prometheus instrumentation and exposes metrics
  `/metrics`.

  - `handler` the ring handler fn
  - `routes` bidi web routes
  - `registry` an instance of IapetosRegistry"
  ([handler routes registry]
   (wrap-metrics handler routes registry {}))
  ([handler routes registry {:keys [ignore-keys] :as options}]
   (let [path-fn (partial path routes ignore-keys)]
     (ring/wrap-metrics handler registry {:path-fn path-fn}))))
