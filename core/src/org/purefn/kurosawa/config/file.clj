(ns org.purefn.kurosawa.config.file
  (:require [org.purefn.kurosawa.config :as config]
            [org.purefn.kurosawa.config.file-impl :as impl]))

(config/set-config-fetcher! impl/fetch-config)
