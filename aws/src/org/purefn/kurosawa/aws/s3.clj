(ns org.purefn.kurosawa.aws.s3
  "Fetch config from AWS S3.

  Expects the environment variable KUROSAWA_S3_CONFIG_URI to be set to something
  like:

  s3://my-bucket/path/to/the/things

  Unencrypted JSON files located at
  - s3://my-bucket/path/to/the/things/configs/config-1.json 
  - s3://my-bucket/path/to/the/things/configs/config-2.json 
  
  and KMS encrypted JSON files located at
  - s3://my-bucket/path/to/the/things/secrets/config-2.json
  - s3://my-bucket/path/to/the/things/secrets/config-3.json

  will be read (merged) into a map like

  {config-1 {key1 value1
             key2 value2}
   config-2 {key3 value3
             key4 value4}
   config-3 {key3 value3
             key4 value4}}"
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [taoensso.timbre :as log])
  (:import
   [com.amazonaws.services.s3.model ListObjectsRequest ObjectListing]
   [com.amazonaws.services.s3 AmazonS3Client AmazonS3ClientBuilder AmazonS3URI]
   [com.amazonaws.services.kms AWSKMSClientBuilder]
   [com.amazonaws.services.kms.model DecryptRequest]
   [java.io File]
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

;; temporarily needed while moving from k8s configs where all things are stringly
;; typed.
(def parse
  (comp 
   (some-fn #(try (Integer. %)
                  (catch Exception ex))
            #(try (Long. %)
                  (catch Exception ex))
            #(try (Double. %)
                  (catch Exception ex))
            identity)
   (fn [s]
     (if (string? s)
       (str/trim s)
       s))))

(def ^:private basename
  (comp
   (fn [s]
     (let [i (.lastIndexOf s ".")]
       (if (pos? i)
         (subs s 0 i)
         s)))
   (memfn getName)
   #(File. %)))

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
      ((juxt (constantly (basename k)) read-json))))

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
       ((juxt (constantly (basename k)) read-json))))

(defn fetch-config
  [bucket path]
  (log/info "Reading config from s3" :bucket bucket :path path)
  (->> (concat (pmap fetch-object
                     (list-objects bucket (str path "configs/")))
               (pmap fetch-encrypted-object
                     (list-objects bucket (str path "secrets/"))))
       (map (fn [[n kvs]]
              [n
               (map (juxt (comp identity key)
                          (comp parse val))
                    kvs)]))
       (reduce (fn [conf [k vs]]
                 (merge-with merge conf {k (into {} vs)}))
               {})))

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
