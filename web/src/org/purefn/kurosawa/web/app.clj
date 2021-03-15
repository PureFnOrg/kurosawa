(ns org.purefn.kurosawa.web.app
   "A component for web apps that need access to services (stateful
   components.) It mainly functions as a container for all the
   components that the web app handlers need. It provides a middleware
   to inject those components into ring requests.

   To use, initialize by calling `app` with a config map (unused for
   now) and a handler function. In your component system map, add any
   other components that your app needs. Call `app-handler` with the
   app component to return a handler wrapped in a middleware that will
   inject all the components into the request map, under a
   `::services` key, keyed by the same keys specified in the system map.

   Handlers can use the `service` helper function to fetch a
   particular service out of a request map."
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [org.purefn.kurosawa.web.ring :as ring]))


;;------------------------------------------------------------------------------
;; Component. 
;;------------------------------------------------------------------------------

(defrecord App
    [config handler])

;;------------------------------------------------------------------------------
;; Middleware
;;
;; This protocol can be implemented and used to apply middleware to the handler
;; function provided to the `App`. The component would then need to be part of
;; the `SystemMap` under the key :middleware.
;;
;; The use case for this type of middleware would be that which is dependent on
;; a stateful component that needs to be used by other components within the
;; `SystemMap`.
;;------------------------------------------------------------------------------

(defprotocol Middleware
  (apply-middleware [this handler]))

(defn wrap-services
  "Middleware that wraps a handler with given services map."
  [h services]
  (fn [req]
    (h (assoc req ::ring/services services))))

(defn app-handler
  "Main API function. Builds handler function from app."
  [app]
  (let [services (dissoc app :config :handler :middleware)
        handler (if-let [mw (:middleware app)]
                  (apply-middleware mw (:handler app))
                  (:handler app))]
    (wrap-services handler services)))

(defn service
  "Returns a service from a request map. Helper function for handlers
   to pull out services assoced in by `wrap-services` middleware."
  [req service-k]
  (get-in req [::ring/services service-k]))


;;------------------------------------------------------------------------------
;; Configuration 
;;------------------------------------------------------------------------------

(defn default-config
   "As much of the default configuration as can be determined from the current
   runtime environment.

   - `name` The root of the ConfigMap and Secrets directory.  Defaults to 
   `app` if not provided."
  ([name] {})
  ([] (default-config "app")))


;;------------------------------------------------------------------------------
;; Creation.
;;------------------------------------------------------------------------------
     
(defn app
  "Returns an App record with provided config and handler."
  [config handler]
  (->App config handler))


;;------------------------------------------------------------------------------
;; Specs. 
;;------------------------------------------------------------------------------

(s/def ::middleware (partial satisfies? Middleware))

(s/def ::services
  (s/map-of keyword? (partial satisfies? component/Lifecycle)))

(s/fdef app
        :args (s/cat :config map? :handler ::ring/handler)
        :ret (partial instance? App))

(s/fdef app-handler
        :args (s/cat :app (partial instance? App))
        :ret ::ring/handler)
