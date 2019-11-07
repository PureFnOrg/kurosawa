(ns org.purefn.kurosawa.web.handler
  (:require [taoensso.timbre :as log]))

(defn not-found
  [{:keys [request-method uri]}]
  (let [msg (format "%s %s not found" request-method uri)]
    (log/warn msg)
    {:status 404
     :body msg}))
