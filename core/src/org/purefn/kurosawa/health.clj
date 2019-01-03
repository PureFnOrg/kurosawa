(ns org.purefn.kurosawa.health)

(defprotocol HealthCheck
  (healthy? [this]))
