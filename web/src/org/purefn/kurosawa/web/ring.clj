(ns org.purefn.kurosawa.web.ring
  "Ring request/response related specs."
  (:require [clojure.spec.alpha :as s]))


;;------------------------------------------------------------------------------
;; Specs. 
;;------------------------------------------------------------------------------

(s/def ::server-port pos-int?)
(s/def ::server-name string?)
(s/def ::remote-addr string?)
(s/def ::uri (s/and string? (partial re-matches #"^/[^?]+$")))
(s/def ::query-string string?)
(s/def ::scheme #{:http :https})
(s/def ::request-method #{:get :post :put :patch :head :delete})
(s/def ::protocol string?)
(s/def ::headers (s/map-of string? string?))
(s/def ::body (s/or :is (partial instance? java.io.InputStream)
                    :str string?
                    :seq (partial instance? clojure.lang.ISeq)
                    :file (partial instance? java.io.File)))
(s/def ::status (s/and pos-int? #(>= % 100)))

(s/def ::ring-req
  (s/keys ::req-un [::server-port ::server-name ::remote-addr
                    ::uri ::scheme ::request-method ::protocol
                    ::headers]
          ::opt-un [::query-string ::body]))

(s/def ::ring-resp
  (s/keys ::req-un [::status ::headers]
          ::opt-un [::body]))

(s/def ::handler
  (s/fspec :args (s/cat :req ::ring-req)
           :ret ::ring-resp))
