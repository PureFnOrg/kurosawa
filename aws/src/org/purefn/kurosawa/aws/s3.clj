(ns org.purefn.kurosawa.aws.s3
  (:require [clojure.data.json :as json])
  (:import
   [com.amazonaws.services.s3.model ListObjectsRequest ObjectListing]
   [com.amazonaws.services.s3 AmazonS3Client AmazonS3ClientBuilder]))

(defn object-seq
  [client response]
  (when-let [keys (->> (.getObjectSummaries response)
                       (map (juxt (memfn getBucketName) (memfn getKey)))
                       (seq))]
    (concat keys (object-seq client (.listNextBatchOfObjects client response)))))

(defn list-objects
  [bucket path]
  (let [client (AmazonS3ClientBuilder/defaultClient)]
    (object-seq
     client
     (.listObjects client
                   (doto (ListObjectsRequest.)
                     (.setBucketName bucket)
                     (.setMaxKeys (Integer. 1))
                     (.setPrefix path)
                     (.setDelimiter "/"))))))
