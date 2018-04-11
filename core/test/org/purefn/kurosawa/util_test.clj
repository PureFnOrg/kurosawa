(ns org.purefn.kurosawa.util-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer [is testing deftest]]
            [org.purefn.kurosawa.util :refer :all]))

#_(stest/instrument [`chain `revertable])

(def num-values 100)

(def small-int? (s/and int? #(< -10000 % 10000)))
(s/def ::times small-int?)
(s/def ::plus small-int?)
(s/def ::minus small-int?)
(s/def ::orig (s/and int? #(< 1 % 10000)))
(def initial? (s/keys :req [::times ::plus ::minus ::orig]))


(defonce result (atom 5))

(def math-chain
  (chain (fn [data]
           (let [{v ::times} data]
             (when-not (zero? v)
               (do 
                 (swap! result #(* % v))
                 [data (fn [] (swap! result #(/ % v)))]))))
         
         (fn [data]
           (let [{v ::plus} data]
             (when-not (odd? v)
               (swap! result #(+ % v))
               [data (fn [] (swap! result #(- % v)))])))
         
         (fn [data]
           (let [{v ::minus} data]
             (swap! result #(- % v))
             [data]))))

(deftest test-chain
  (testing "Executing chained operations"
    (doseq [{:keys [::times ::plus ::minus ::orig] :as initial}
            (gen/sample (s/gen initial?) num-values)]
        (reset! result orig)
        (math-chain initial :debug false)
        (if (or (zero? times)
                (odd? plus))
          (is (= @result orig))
          (is (= @result (-> orig (* times) (+ plus) (- minus))))))))
