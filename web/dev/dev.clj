(ns dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application.

  Call `(reset)` to reload modified code and (re)start the system.

  The system under development is `system`, referred from
  `com.stuartsierra.component.repl/system`.

  See also https://github.com/stuartsierra/component.repl"
  (:require
   [bidi.bidi :as bidi]
   [bidi.ring]
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.set :as set]
   [clojure.string :as string]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer [refresh refresh-all clear]]
   [com.stuartsierra.component :as component]
   [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
   [org.purefn.kurosawa.web.prometheus :as prometheus]
   [org.purefn.kurosawa.web.server :as server]
   [ring.middleware.keyword-params :refer (wrap-keyword-params)]
   [ring.middleware.multipart-params :refer (wrap-multipart-params)]
   [ring.middleware.params :refer (wrap-params)]
   [ring.util.response :as response]
   [org.purefn.kurosawa.web.app :as app]))

;; Do not try to load source code from 'resources' directory
(clojure.tools.namespace.repl/set-refresh-dirs "dev" "src" "test")

(def test-routes
  [""
   [["/"
     {"api/some-resource/v1/"
      {:get {["fetch/" :id] (constantly {:status 200
                                         :body {:result "ok"}})}}}]]])

(defn handler
  []
  (-> (bidi.ring/make-handler test-routes)
      (prometheus/wrap-metrics
       test-routes
       prometheus/registry {:ignore-keys [:version]})))

(defn dev-system
  "Constructs a system map suitable for interactive development."
  []
  (component/system-map
   :app (app/app (app/default-config)
                 (handler))
   :server (component/using
            (server/immutant-server (server/default-config))
            [:app])))

(set-init (fn [_] (dev-system)))
