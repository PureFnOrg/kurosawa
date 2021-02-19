(ns org.purefn.kurosawa.aws.s3-test
  (:require [clojure.test :refer :all]
            [org.purefn.kurosawa.aws.s3 :as s3]))

;; These tests show the config parsing contract.  The config source
;; originally started as files on disk, then k8s secrets, and is
;; finally now json in s3 and environment vars.
(deftest parsing
  (testing "Integer parsing"
    (is (= 10000 (s3/parse "10000")))
    (is (= 10000 (s3/parse "10000 ")))
    (is (= 10000 (s3/parse 10000))))
  (testing "Double parsing"
    (is (= 10000.0 (s3/parse "10000.0")))
    (is (= 10000.0 (s3/parse 10000.0))))
  (testing "String parsing"
    (is (= "anything else" (s3/parse "anything else")))))

