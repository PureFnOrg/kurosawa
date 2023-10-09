(ns org.purefn.kurosawa.log.core
  "Standardized logging configuration."
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [com.stuartsierra.dependency :as dep]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as append]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [org.purefn.kurosawa.log.api :as api]
            [org.purefn.kurosawa.log.datadog :as dd]))

(def prod-dir
  "The root directory under which production log files are written."
  "/var/log/kurosawa")

(defn- norm
  [value]
  (cond
    (nil? value) "nil"
    (keyword? value) (name value)
    :default value))

(defn format-msg
  "Generate a formatted string suitable for log messages.
   
   - `msg` The main log message text.
   - `params-map` A map of information (usually function parameters) to include
   as part of the formatted message in a standardized form easy for log parsing." 
  [msg params-map]
  (->> params-map
      (map (fn [[k v]] (str (norm k) "=" (norm v))))
      (str/join " ")
      (format "%s [%s]" msg)))

(defmacro envmap
  "A map of all the symbols and their values currently bound in the environment."
  [] 
  (let [syms (keys &env)]
    `(into {} (map vector ~(mapv keyword syms) (vector ~@syms)))))

(defn fn-trace
  "Log the invocation of a function at `:trace` level along with its arguments. 

   - `fn-name` The name of the function being invoked as a keyword.
   - `args`    A map of the function argument names (as keyword) and values.
   
   This function should be called (for side-effects only) at the start of the
   function being traced."
  [fn-name args]
  (when (log/may-log? :trace)
    (->> args 
         (filter (fn [[k _]] (not= k :this)))
         (format-msg (str "API " fn-name))
         (log/trace))))

(defn output-fn 
  "Default logging output function.

   Use `(partial output-fn <opts-map>)` to modify default opts."
  ([     data]
   (output-fn nil data))
  ([opts data] 
   (let [{:keys [no-stacktrace? stacktrace-fonts]} opts
         {:keys [level ?err msg_ ?ns-str ?file hostname_
                 timestamp_ ?line]} data]
     (str (force timestamp_) " "
          (str/upper-case (name level))  " "
          "[" (or ?ns-str ?file "?") ":" (or ?line "?") "] - "
          (force msg_)
          (when-not no-stacktrace?
            (when-let [err ?err]
              (str "\n" (log/stacktrace err opts))))))))

(defn base-config
   "Create a base logging configuration for all Kurosawa libraries and apps
    for use in a production context.

   - `dir` The root logging directory, defaults to `prod-dir` if not 
   specified."
  ([]
   (base-config prod-dir))
  ([dir]
   {:level :info
    :ns-whitelist []
    :ns-blacklist []
    :middleware []
    :timestamp-opts {:pattern "yy-MM-dd'T'HH:mm:ss'Z'"
                     :locale :jvm-default
                     :timezone (java.util.TimeZone/getTimeZone "America/New_York")}
    :output-fn output-fn
    :appenders {:stdout (if dd/available?
                          (dd/println-json-appender)
                          (append/println-appender :stream :auto))
                :all (rotor/rotor-appender {:path (str dir "/all.log")
                                            :max-size (* 8 1024 1024)
                                            :backlog 10})}}))
(defn- unique-ns
  [orig-nss nss]
  (->> (set orig-nss)
       (clojure.set/union (set nss))
       (into [])))  

(defn add-component-appender
  "Generates a function which will add a component specific rotating log 
   appender to a Timbre log configuration. 
   
   - `appender-kw` The keyword identifier for the created component appender.
   - `namespaces` A sequence of string patterns (`\"com.foo.*\"` ...) used to 
   direct log messages for matching namespaces to the created appender and 
   away from the `:stdout` log.
   - `filename` The name of the created log file.

   Also accepts the following appender specific options: 

   - `max-size` The maximum size a log file can grow (in bytes) before being
   rotated.
   - `backlog` The number of rotating logs to retain.

   Returns a function which takes Timbre log configuration map and returns
   the modified configuration."  
  [appender-kw namespaces filename & {:as rotor-opts}]
  (let [ropts (merge {:max-size (* 8 1024 1024)
                      :backlog 10}
                     rotor-opts
                     {:path filename})]
    (fn [config]
      (-> config
          (update-in [:appenders :stdout :ns-blacklist]
                     unique-ns namespaces)
          (update-in [:appenders]
                     assoc appender-kw (-> (rotor/rotor-appender ropts)
                                           (assoc :ns-whitelist namespaces)))))))

(defn system-log-configure
  "Generate a single function which combines all logging configuration for all 
   components in a system.

   - `system` The initialized component system.
   - `dir` The root logging directory.

   Returns a function which takes Timbre log configuration map and returns
   the modified configuration."  
  [system dir]
  (let [ks (keys system)
        graph (component/dependency-graph system ks)]
    (->> (sort (dep/topo-comparator graph) ks)
         (map (fn [k] (api/log-configure (k system) dir)))
         (apply comp))))

(defn system-log-namespaces
   "Collect the namespace patterns from the named components of the system.

   - `system` The initialized component system.
   - `comp-keys` The keyword identifiers of selected components, if not 
   specified all components are selected."
  ([system]
   (system-log-namespaces system (keys system)))
  ([system comp-keys]
   (let [graph (component/dependency-graph system comp-keys)]
     (->> (sort (dep/topo-comparator graph) comp-keys)
          (reduce (fn [ns k] (concat ns (api/log-namespaces (k system)))) #{})
          (into [])))))

(defn init-prod-logging
  "Initialize production logging for a component system.

   Only high-level component logging will be sent to the console. 
   Per-component low-level logging will be written to separate rolling log 
   files. See individual components for details of the names of these log 
   files. Finally, all logging will be consolidated in a single rolling log 
   file called `all.log`.

   - `system` The initialized component system.
   - `dir` The root logging directory, defaults to `prod-dir` if not 
   specified.
  
   This is a side-effecting function that modifies and returns the 
   `taoensso.timbre/*config*` value."
  ([system]
   (init-prod-logging system prod-dir))
  ([system dir]
   (->> (base-config dir)
        ((system-log-configure system dir))
        (log/set-config!))))

(defn init-dev-logging
  "Initialize development logging for a component system.

   With no arguments, both the low-level and high-level logging will be sent 
   to the console.  

   With a single `system` argument, only high-level logging will be sent to 
   the console.  All low-level logging, as defined by `(log-namespaces)` on 
   the components will be omitted.

   If both a `system` and `excluded-comp-keys` are given, then some low-level 
   logging will be omitted based on the excluded components.  High-level 
   logging will always be included.

   - `system` The initialized component system.
   - `excluded-comp-keys` A sequence of keyword identifiers of components for 
   which low-level logging should be excluded.
  
   This is a side-effecting function that modifies and returns the 
   `taoensso.timbre/*config*` value."
  ([]
   (init-dev-logging nil []))
  ([system]
   (init-dev-logging system (keys system)))
  ([system excluded-comp-keys]
   (-> (base-config)
       (update-in [:appenders] dissoc :all)
       ((fn [cfg]
          (if system
            (->> (system-log-namespaces system excluded-comp-keys)
                 (update-in cfg [:ns-blacklist] unique-ns))
            cfg)))
       (log/set-config!))))

(defn set-level
  "Set the default logging level. 
   
   - `level` Possible values: `:trace` `:debug` `:info` `:warn` `:error`
  `:fatal` `:report`

   Useful shortcut for temporarily changing logging level from the REPL."
  [level]
  (log/set-config! (assoc log/*config* :level level)))
