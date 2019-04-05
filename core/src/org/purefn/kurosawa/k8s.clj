(ns org.purefn.kurosawa.k8s
  "Kubernetes configuration and deployment helpers."
  (:require [org.purefn.kurosawa.config.file-impl :as file]))

;;------------------------------------------------------------------------------
;; API
;;------------------------------------------------------------------------------

(defn kubernetes?
  "[DEPRECATED] -- always returns true.

  Is the program currently running in a Kubernetes container?

  This was/is a leaky abstraction, the application shouldn't be concerned with
  where/how it is being run."
  []
  true)

(defn config-map
  "Read the Kubernetes ConfigMap from container local disk.

   - `name` The base name of the configuration (if any).
   
   Returns a nested map of configuration parameters."
  ([name]
   (file/fetch-config (str "/etc/configs/" name)))
  ([]
   (config-map "")))

(defn secrets
  "Read the Kubernetes Secrets from container local disk.

   - `name` The base name of the secrets (if any).
   
   Returns a nested map of plain text secrets."
  ([name]
   (file/fetch-config (str "/etc/secrets/" name)))
  ([]
   (secrets "")))


;;------------------------------------------------------------------------------
;; Specs. 
;;------------------------------------------------------------------------------
