(ns org.purefn.kurosawa.log.protocol
  "Protocol definitions.")

(defprotocol Logging
   "Logging configuration for components."
   (log-namespaces [this])
   (log-configure [this dir]))

