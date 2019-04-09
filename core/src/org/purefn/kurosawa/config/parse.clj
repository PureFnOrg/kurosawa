(ns org.purefn.kurosawa.config.parse
  (:require [org.purefn.kurosawa.result :refer :all]))

(defn value
  "Attempt to coerce a String into an Integer, a Long, a Double, then finally gives
  up and returns the original String."
  [v]
  (-> (attempt (fn [^String s] (Integer. s)) v)
      (recover (fn [_] (Long. v)))
      (recover (fn [_] (Double. v)))
      (recover (constantly v))
      (success)))
