(ns org.purefn.kurosawa.util
  "Miscellaneous utility functions."
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]))

(defn flip
  "Returns a function which applies its arguments in reversed order from the
   original function `f`.

   ```(= ((flip vector) 1 2 3) [3 2 1])```"
  [f]
  (fn 
    ([] (f))
    ([x] (f x)) 
    ([x y] (f y x)) 
    ([x y z] (f z y x)) 
    ([a b c d] (f d c b a)) 
    ([a b c d & rest]
        (->> rest
             (concat [a b c d])
             reverse
             (apply f)))))

(defn chain
  "Returns a function that when executed will perform a reversable process 
   composed of a sequence of steps.

   - `steps`   A sequence of 1 argument functions with side effects.
   
   On success, each step function should return a vector `[result revert-fn]`
   where:

   - `result`    If non-nil, the value passed to the next step in the process.
   - `revert-fn` If non-nil, a no-arg function which when executed will 
                 reverse the side effects of the step function.

   The chain process proceeds by executing the first step function with the 
   `initial` value.  If the `result` part of the return value is non-nil, 
   this value will be used as the argument for the next step function in the
   sequence.  This repeats until all step functions have been executed 
   successfully. 

   If during this process the a step function fails, the `revert-fn` function
   for all previous steps will be called in the reverse order of execution.
   This way all side effects of the entire process will be safely undone. Any 
   previous step's `revert-fn` who's value is nil will be ignored.

   A step function is considered to have failed if: 

   - The `result` portion of the value returned by the step function is nil.
   - The entire return value of the step function is nil.
   - The step function throws an exception. 

   The function returned will take the following arguments: 

   - `initial` The value passed to the first step.
   - `opts`    Optional arguments: `:debug true` turns on verbose logging.

   When executed, it will return the `result` portion of the last successful 
   step or nil if there is a failure and the process is reverted."
  [& steps]
  (fn [initial & {:keys [debug]}]
    (let [g (fn f [value ss rs cnt]
              (if-let [apply-fn (first ss)]
                (do 
                  (let [[result revert-fn] (try (apply-fn value)
                                                (catch Exception ex nil))
                        _ (when debug
                            (if result
                              (log/info "Applied step" cnt)
                              (log/warn "Failed to apply step" cnt)))
                        nrs (cons [revert-fn cnt] rs)]
                    (if result
                      (f result (rest ss) nrs (inc cnt))
                      (doseq [[rf c] nrs]
                        (if rf
                          (try (rf)
                               (when debug
                                 (log/info "Successfully reverted step" c))
                               (catch Exception ex
                                 (when debug
                                   (log/warn "Failed to revert step" c))))
                          (when debug
                            (log/info "Revert not needed for step" c)))))))
                value))]
      (g initial steps (list) 0))))

(defn revertible
  "Takes an application function and revert function, and returns a
   revertible function suitable for use as part of a `chain`."
  [apply-fn revert-fn]
  (juxt apply-fn (constantly revert-fn)))

(defmacro defprinter
  "Overrides the print methods of class `c` with function `f`.

  Useful for avoiding the printing of large `defrecord`s in logs and the REPL."
  [c f]
  `(do
     (defmethod print-method ~c [c# w#]
       (print-simple (~f c#) w#))

     (defmethod print-dup ~c [c# w#]
       (print-simple (~f c#) w#))

     (defmethod pprint/simple-dispatch ~c [c#]
       (.write *out* (~f c#)))))

;;------------------------------------------------------------------------------
;; Specs
;;------------------------------------------------------------------------------

(def unary? (s/fspec :args (s/cat :v some?)
                     :ret any?))

(def step? (s/fspec :args (s/cat :v some?)
                    :ret (s/tuple any? unary?)))

(s/fdef chain
        :args (s/coll-of step?)
        :ret any?)

(s/fdef revertible
        :args (s/cat :apply-fn unary? :revert-fn unary?)
        :ret step?)

