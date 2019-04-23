(ns org.purefn.kurosawa.aws.s3
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [taoensso.timbre :as log])
  (:import
   [com.amazonaws.services.s3.model ListObjectsRequest ObjectListing]
   [com.amazonaws.services.s3 AmazonS3Client AmazonS3ClientBuilder]
   [com.amazonaws.services.kms AWSKMSClientBuilder]
   [com.amazonaws.services.kms.model DecryptRequest]
   [java.nio ByteBuffer]
   [java.util Base64]))

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
                     (.setPrefix path)
                     (.setDelimiter "/"))))))

(defn fetch-object
  [[bucket k]]
  (-> (.getObject (AmazonS3ClientBuilder/defaultClient) bucket k)
      (.getObjectContent)
      (slurp)
      (json/read-json)))

(defn fetch-encrypted-object
  [[bucket k]]
  (->> (.getObject (AmazonS3ClientBuilder/defaultClient) bucket k)
       (.getObjectContent)
       (slurp)
       (str/trim)
       (.decode (Base64/getDecoder))
       (ByteBuffer/wrap)
       ((fn [buffer]
          (doto (DecryptRequest.)
            (.setCiphertextBlob buffer))))
       (.decrypt (AWSKMSClientBuilder/defaultClient))
       (.getPlaintext)
       (.array)
       (String.)
       (json/read-json)))

(defn fetch-config
  [bucket path]
  (->> (concat (map fetch-object
                    (list-objects bucket path))
               (pmap fetch-encrypted-object
                     (list-objects bucket (str path "secrets/"))))
       ))

(defn fetch
  []
  (let [bucket (System/getenv "KUROSAWA_S3_CONFIG_BUCKET")
        path (System/getenv "KUROSAWA_S3_CONFIG_PATH")]
    (if-not (and bucket path)
      (log/warn "Environment variables for fetching config not found."
                "Both KUROSAWA_S3_CONFIG_BUCKET and KUROSAWA_S3_CONFIG_PATH"
                "must be set!"
                :bucket bucket :path path)
      (fetch-config bucket path))))
