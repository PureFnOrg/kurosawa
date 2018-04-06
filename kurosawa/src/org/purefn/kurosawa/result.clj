(ns org.purefn.kurosawa.result
  "Option monad style exception handling.")

;;------------------------------------------------------------------------------
;; Success
;;------------------------------------------------------------------------------

(deftype Success [value]
  Object
  (equals [self other]
    (if (instance? Success other)
      (= value (.-value ^Success other))
      false)))

(alter-meta! #'->Success assoc :private true)

(defn succeed
  "A successfully computed value."
  [value]
  (Success. value))


;;------------------------------------------------------------------------------
;; Failure 
;;------------------------------------------------------------------------------

(deftype Failure [ex]
  Object
  (equals [self other]
    (if (instance? Failure other)
      (= ex (.-ex ^Failure other))
      false)))

(alter-meta! #'->Failure assoc :private true)

(defn fail
  "The reason (Exception) for the failure."
  [ex]
  (Failure. ex))


;;------------------------------------------------------------------------------
;; Functions
;;------------------------------------------------------------------------------

(defn success?
  "Whether the value is a `Success`."
  [result]
  (cond
    (instance? Failure result) false
    (instance? Success result) true
    :default nil))

(defn failure?
  "Whether the value is a `Failure`."
  [result]
  (cond
    (instance? Failure result) true
    (instance? Success result) false
    :default nil))

(defn result?
  "Whether the value is a `Success` or `Failure`."
  [result]
  (or (failure? result)
      (success? result)))      

(defn success
  "Returns the value wrapped by the `Success`, or nil."
  [result]
  (when (instance? Success result)
    (.-value ^Success result)))

(defn failure
  "Returns the exception wrapped by the `Failure`, or nil."
  [result]
  (when (instance? Failure result)
    (.-ex ^Failure result)))

(defn result
  "Returns the value wrapped by the `Success`, the exception wrapped by
   the `Failure`, or nil."
  [result]
  (or (failure result)
      (success result)))

(defn attempt
  "Attempt a computation which may fail by throwing Exceptions.

   Apply the function `f` to the given arguments `args`.

   - If successful, returns the resulting value wrapped in a `Success`.
   - If an exception occurs, returns the exception wrapped in a `Failure`."
  [f & args]
  (try (let [result (apply f args)]
         (if (result? result)
           result
           (succeed result)))
       (catch Exception e# (fail e#))))

(defn proceed
  "Proceed with a computation after a previous `Success`.

   If the `result` is:

   - a `Failure`, return it as-is. 
   - a `Success`, return the results of applying the function `f` to the value 
   wrapped in the `Success` along with any additional `args` provided."
  [result f & args]
  (if (failure? result)
    result
    (apply attempt f (success result) args)))

(defn proceed-all
  "Proceed with a computation after a multiple previous `Success`s.

   In addition to the `result`, one or more of the additional arguments `args`
   may also be a Result type. The computation will only proceed if all of these
   Result values are a `Success`. If any is a `Failure`, the first encountered
   will be returned as-is. Otherwise, all values wrapped in a `Success` will
   be extracted and the results of applying the function `f` to these values
   will be returned."
  [result f & args]
  (let [as (concat [result] args)]
    (if-let [fr (some (fn [r] (when (failure? r) r)) as)]
      fr
      (->> as
           (map (fn [r] (if (success? r) (success r) r)))
           (apply attempt f)))))

(defn recover
  "Recover from a previous `Failure`. 

   If the `result` is:

   - a `Failure`, return the result of applying the function `f` to the 
   exception wrapped in the `Failure` along with any additional arguments 
   `args` provided.
   - a `Success`, return it as-is."
  [result f & args]
  (if (failure? result)
     (apply attempt f (failure result) args)
     result))

(defn branch
  "Continue computation after a previous `Success` or `Failure`.

   If `result` is: 

   - a `Failure`, return the result of applying the function `f` to the 
   exception wrapped in the Failure.
   - a `Success`, return the result of applying the `g` function to the value
   wrapped in the `Success`."
  [result f g]
  (if (failure? result)
    (attempt f (failure result))
    (attempt g (success result))))

(defn flatten-results
  "Convert a sequence of Results into a Result containing a sequence.

   - If any of the elements of the sequence are a `Failure`, then the values 
   for each of the failures will be returned in a seq inside a single Failure 
   object. Otherwise a `Success` will be returned containing the unwrapped 
   (all successful) elements."
  [rs]
  (if-let [fr (some failure? rs)]
    (->> (filter failure? rs)
         (map failure)
         (fail))
    (succeed (map success rs))))


;;------------------------------------------------------------------------------
;; Specs
;;------------------------------------------------------------------------------

