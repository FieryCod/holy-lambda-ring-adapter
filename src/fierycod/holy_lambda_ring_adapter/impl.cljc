(ns fierycod.holy-lambda-ring-adapter.impl
  (:import
   [java.io InputStream File ByteArrayInputStream]
   [java.util Base64 Base64$Decoder Base64$Encoder]
   [java.net URL]
   [java.nio.file Files]
   [clojure.lang ISeq IPersistentMap PersistentVector IPersistentSet])
  #?(:bb
     (:require
      [clojure.string :as s]
      [clojure.java.io :as io])
     :clj
     (:require
      [ring.util.response :as resp]
      [clojure.string :as s]
      [clojure.java.io :as io])))

#?(:bb nil
   :clj (set! *warn-on-reflection* true))

;; See https://github.com/ring-clojure/ring/pull/447/files
#?(:bb nil
   :clj
   (defmethod resp/resource-data :resource
     [^URL url]
     ;; GraalVM resource scheme
     (let [resource (.openConnection url)]
       {:content        (.getInputStream resource)
        :content-length (#'ring.util.response/connection-content-length resource)
        :last-modified  (#'ring.util.response/connection-last-modified resource)})))

(def ^:private base64-decoder (delay (Base64/getDecoder)))
(def ^:private base64-encoder (delay (Base64/getEncoder)))

(defprotocol RingResponseBody
  (to-hl-response-body [body] "Adapts the RingResponseBody to valid HLResponseBody"))

(extend-protocol RingResponseBody
  ByteArrayInputStream
  (to-hl-response-body [^ByteArrayInputStream body]
    {:body     (.readAllBytes body)
     :encoded? true})

  InputStream
  (to-hl-response-body [^InputStream body]
    {:body     (new String (.readAllBytes body))
     :encoded? false})

  File
  (to-hl-response-body [^File body]
    {:body     (.encode ^Base64$Encoder @base64-encoder ^bytes (Files/readAllBytes (.toPath body)))
     :encoded? true})

  String
  (to-hl-response-body [^String body]
    {:body     body
     :encoded? false})

  URL
  (to-hl-response-body [^URL body]
    {:body     (.encode ^Base64$Encoder @base64-encoder ^bytes (.readAllBytes (.getInputStream (.openConnection body))))
     :encoded? true})

  IPersistentSet
  (to-hl-response-body [^IPersistentSet body]
    {:body     body
     :encoded? false})

  PersistentVector
  (to-hl-response-body [^PersistentVector body]
    {:body     body
     :encoded? false})

  IPersistentMap
  (to-hl-response-body [^IPersistentMap body]
    {:body     body
     :encoded? false})

  ISeq
  (to-hl-response-body [^ISeq body]
    {:body     (s/join "\n" (map str body))
     :encoded? false})

  Object
  (to-hl-response-body [^Object body]
    {:body     (str body)
     :encoded? false})

  nil
  (to-hl-response-body [_]
    {:body     nil
     :encoded? false}))

(defprotocol HLRequestBody
  (to-ring-request-body [body base64?] "Adapts the HLRequestBody to valid RingRequestBody"))

(extend-protocol HLRequestBody
  String
  (to-ring-request-body [^String body base64?]
    (io/input-stream
     (if-not base64?
       (.getBytes body)
       (.decode ^Base64$Decoder @base64-decoder ^String body))))

  Object
  (to-ring-request-body [^Object body base64?]
    (pr-str body))

  nil
  (to-ring-request-body [_ _]
    nil))
