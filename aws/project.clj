(defproject org.purefn/kurosawa.aws "2.1.10"
  :plugins [[lein-modules "0.3.11"]]
  :description "AWS Utilities."
  :dependencies [[com.amazonaws/aws-java-sdk-core "1.11.533"]
                 [com.amazonaws/aws-java-sdk-ssm "1.11.533"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.533"]
                 [org.clojure/data.json "0.2.6"]
                 [com.taoensso/timbre _]])
