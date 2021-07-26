(ns org.purefn.kurosawa.web.prometheus
  "Includes a prometheus ring middleware using iapetos and bidi.  Bidi's route
  matching is required to remove route params from the time series.

  This library does not include iapetos and bidi as dependencies directly, so
  requiring this namespace will break unless you've done so yourself."
  (:require [bidi.bidi :as bidi]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [iapetos.core :as prometheus]
            [iapetos.collector.ring :as ring]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:import [iapetos.registry IapetosRegistry]))

(defn path
  [routes ignore-keys {:keys [uri request-method]}]
  (or (->> (bidi/match-route routes uri :request-method request-method)
           (:route-params)
           (remove (comp (set ignore-keys)
                         first))
           (reduce (fn [path [p v]]
                     (str/replace path v (str p)))
                   uri))
      uri))

(defonce registry
  (-> (prometheus/collector-registry)
      (ring/initialize)))

(defn wrap-metrics
  ([handler routes registry]
   (wrap-metrics handler routes registry {}))
  ([handler routes registry {:keys [ignore-keys] :as options}]
   (let [path-fn (partial path routes ignore-keys)]
     (ring/wrap-metrics handler registry {:path-fn path-fn}))))


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
