(ns org.purefn.kurosawa.aws.s3
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [taoensso.timbre :as log])
  (:import
   [com.amazonaws.services.s3.model ListObjectsRequest ObjectListing]
   [com.amazonaws.services.s3 AmazonS3Client AmazonS3ClientBuilder AmazonS3URI]
   [com.amazonaws.services.kms AWSKMSClientBuilder]
   [com.amazonaws.services.kms.model DecryptRequest]
   [java.nio ByteBuffer]
   [java.util Base64]))

(defn- append-trailing-slash
  [s]
  (if-not (= \/ (last s))
    (str s "/")
    s))

(defn- read-json
  [s]
  (json/read-json s false))

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
      (read-json)))

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
       (read-json)))

(defn fetch-config
  [bucket path]
  (log/info "Reading config from s3" :bucket bucket :path path)
  (->> (concat (mapcat fetch-object
                       (list-objects bucket path))
               (pmap fetch-encrypted-object
                     (list-objects bucket (str path "secrets/"))))
       (map (fn [m]
              [(m "name")
               (->> (m "data")
                    (map (juxt (comp identity key)
                               (comp str/trim val))))]))
       (reduce (fn [conf [k vs]]
                 (merge-with merge conf {k (into {} vs)}))
               {})))

(defn- append-trailing-slash
  [s]
  (if-not (= \/ (last s))
    (str s "/")
    s))

(defn fetch
  ([]
   (fetch (System/getenv "KUROSAWA_S3_CONFIG_URI")))
  ([uri]
   (let [[bucket path] (some-> uri
                               (AmazonS3URI.)
                               ((juxt (memfn getBucket)
                                      (comp append-trailing-slash
                                            (memfn getKey)))))]
     (if-not (and bucket path)
       (log/warn "Environment variable for fetching config not found,"
                 "KUROSAWA_S3_CONFIG_URI must be set!")
       (fetch-config bucket path)))))
