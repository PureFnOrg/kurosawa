(ns org.purefn.kurosawa.error
  "Error handling utility functions." 
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [org.purefn.kurosawa.log.core :as klog]
            [org.purefn.kurosawa.result :refer :all])
  (:import [java.util.concurrent ThreadLocalRandom]))

(defn snafu
  "Creates a function which given an exception, will generate a log message and
   return a Failure containing the enhanced exception.

   - `reason`    A function which maps exceptions to high-level reason keywords.
   - `reason-kw` The component API namespaced keyword for exception reasons.
   - `fatal-kw`  The component API namespaced keyword for fatal exceptions.
   - `msg`       The error message.
   - `mp`        Map of additional data added to `ex-info`.

   The returned function takes:

   - `ex`  The original exception.

   Typically this is used to create a component specific `snafu` function used 
   internally by the component like so: 

   `(defn reason [ex] ...)`

   `(def snafu (partial error/snafu reason ::api/reason ::api/fatal))`"
  [reason reason-kw fatal-kw msg mp]
  (fn [ex] 
    (let [rsn (reason ex)
          smp (assoc mp reason-kw rsn)
          exi (ex-info msg smp ex)]
      (if (= rsn ::fatal)
        (log/error exi)
        (log/warn (klog/format-msg msg smp)))
      (fail exi))))

(defn- random-sleep
  [max-duration-ms]
  (-> (ThreadLocalRandom/current)
      (.nextInt max-duration-ms)
      (max 1)
      (Thread/sleep)))

(defn retry-generic
  "Retry the supplied no-arg `f` until it succeeds or exhausts the number of 
   retries. 

   - `reason`    A function which maps exceptions to high-level reason keywords.
   - `reason-kw` The component API namespaced keyword for exception reasons.
   - `initial-delay-ms` The amount of initial delay (in ms) when retring.
   - `max-retries` The maximum number of retries before giving up.
   - `f` The function to retry until it succeeds.
   - `recovery` A map of reasons for retrying and associated delay modification
   functions (see below).

   The function `f` will be retried when the reason for the failure is one
   of the keys in the `recovery` map.  The value for the matching key is a 
   function which will produce the next delay given the current one.
   
   Returns the result of `f`, or a non-recoverable `Failure`.

   Typically this is used to create a component specific `retry-generic` 
   function used internally by the component like so: 

   `(defn reason [ex] ...)`

   `(def retry-generic (partial error/retry-generic reason ::api/reason 1 10))`"
  [reason reason-kw initial-delay-ms max-retries f recovery]
  (loop [cnt 1
         ms initial-delay-ms]
    (let [g (fn [ex]
              (if (<= cnt max-retries)
                (if-let [adjust (get recovery (reason ex))]
                  (do (log/warn (format "Retry %s in %s ms." cnt ms))
                      (random-sleep ms)
                      (fail [nil (adjust ms)]))
                  (fail [ex nil]))
                (fail [(ex-info "Max retries reached!"
                                {reason-kw max-retries} ex) nil])))
          result (recover (f) g)]
      (if (success? result)
        result
        (let [[ex nms] (failure result)]
          (if ex
            (fail ex)
            (recur (inc cnt) nms)))))))
