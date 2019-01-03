(ns org.purefn.kurosawa.health
  "Contains a health check protocol which ought to be implemented by components whose
  state might be healthy or unhealthy.  Most commonly testing for the liveness of an
  underlying datastore.  This could be useful with a Kubernetes liveness probe,
  for instance.")

(defprotocol HealthCheck
  (healthy? [this]))
