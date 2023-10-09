(ns org.purefn.kurosawa.log.datadog
  (:require
   [taoensso.timbre :as timbre]
   [clojure.data.json :as json]))

;; `compile-if` sourced from https://github.com/clojure/core.logic/blob/10ee95eb2bed70af5bc29ea3bd78b380f054a8b4/src/main/clojure/clojure/core/logic/datomic.clj#L1
(defmacro compile-if
  "Evaluate `exp` and if it returns logical true and doesn't error, expand to
  `then`.  Else expand to `else`.
  (compile-if (Class/forName \"java.util.concurrent.ForkJoinTask\")
    (do-cool-stuff-with-fork-join)
    (fall-back-to-executor-services))"
  [exp then else]
  (if (try (eval exp)
           (catch Throwable _ false))
    `(do ~then)
    `(do ~else)))

(compile-if (Class/forName "datadog.trace.api.CorrelationIdentifier")
            (do
              (import 'datadog.trace.api.CorrelationIdentifier)
              (defn get-context []
                {:dd-trace-id (datadog.trace.api.CorrelationIdentifier/getTraceId)
                 :dd-span-id (datadog.trace.api.CorrelationIdentifier/getSpanId)})
              (def available? true))
            (do
              (defn get-context [] {})
              (def available? false)))

(defn data->json-string
  [data]
  (json/write-str
   (merge (:context data)
          (get-context)
          {:level (:level data)
           :namespace (:?ns-str data)
           :file (:?file data)
           :line (:?line data)
           :stacktrace (some-> (:?err data) (timbre/stacktrace))
           :hostname (force (:hostname_ data))
           :message (force (:msg_ data))
           "@timestamp" (inst-ms (:instant data))})))

(defn println-json-appender
  "Returns a DataDog appender, which will send each event in JSON
  format to stdout. If DataDog tracing library is injected, `dd-trace-id`
  and `dd-span-id` will be added to the JSON message."
  [& [_opts]]
  {:enabled?   true
   :async?     false
   :min-level  nil
   :rate-limit nil
   :output-fn  data->json-string
   :fn
   (fn [{:keys [output_] :as _data}]
     (println (force output_)))})
